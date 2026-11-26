package com.dartspractice.app.data.backup

import com.dartspractice.app.data.db.DartThrowEntity
import com.dartspractice.app.data.db.MenuEntity
import com.dartspractice.app.data.db.SessionEntity
import com.dartspractice.app.domain.rule.PracticeRules
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

// ---- JSONファイルの形(darts-data.json)。SPEC.md の例に finishedAt と exportedAt を足したもの ----

@Serializable
data class BackupJson(
    val version: Int,
    val exportedAt: String? = null,
    val menus: List<BackupMenuJson>,
    val sessions: List<BackupSessionJson>,
)

@Serializable
data class BackupMenuJson(val id: Long, val name: String, val ruleType: String)

@Serializable
data class BackupSessionJson(
    val id: Long,
    val menuId: Long,
    val startedAt: String,
    val finishedAt: String? = null,
    val finished: Boolean,
    val memo: String = "",
    val throws: List<BackupThrowJson>,
)

@Serializable
data class BackupThrowJson(val seq: Int, val target: String, val rawMarks: Int, val marks: Int)

// ---- DBに入れる形 ----

/** 練習1回分(投は通し番号順)。 */
data class BackupSession(val session: SessionEntity, val throws: List<DartThrowEntity>)

/** バックアップの中身。exportedAt は書き出した日時(ミリ秒)。 */
data class BackupData(
    val menus: List<MenuEntity>,
    val sessions: List<BackupSession>,
    val exportedAt: Long? = null,
)

/** 読み込めないファイルのとき。message は画面にそのまま出す理由。 */
class BackupFormatException(message: String) : Exception(message)

/**
 * バックアップ(JSON)の書き出しと読み込み。
 * 読み込みでは中身を確かめ、1投ごとの記録はルール判定に通して作り直す(手で書き換えたファイルでもおかしな記録は入らない)。
 */
object BackupCodec {
    const val VERSION = 1
    const val FILE_NAME = "darts-data.json"

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    private val dateTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun encode(data: BackupData, exportedAt: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        fun time(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toOffsetDateTime()
            .truncatedTo(ChronoUnit.SECONDS).format(dateTime)

        val file = BackupJson(
            version = VERSION,
            exportedAt = time(exportedAt),
            menus = data.menus.map { BackupMenuJson(it.id, it.name, it.ruleType) },
            sessions = data.sessions.map { (s, throws) ->
                BackupSessionJson(
                    id = s.id,
                    menuId = s.menuId,
                    startedAt = time(s.startedAt),
                    finishedAt = s.finishedAt?.let(::time),
                    finished = s.finished,
                    memo = s.memo,
                    throws = throws.sortedBy { it.seq }.map { BackupThrowJson(it.seq, it.target, it.rawMarks, it.marks) },
                )
            },
        )
        return json.encodeToString(BackupJson.serializer(), file)
    }

    /** @throws BackupFormatException 読み込めないとき(理由付き) */
    fun decode(text: String): BackupData {
        val file = try {
            json.decodeFromString(BackupJson.serializer(), text)
        } catch (e: SerializationException) {
            throw BackupFormatException("ダーツ練習のバックアップファイルではないか、形式が壊れています")
        } catch (e: IllegalArgumentException) {
            throw BackupFormatException("ダーツ練習のバックアップファイルではないか、形式が壊れています")
        }

        check(file.version == VERSION) { "対応していない版のファイルです(version ${file.version})" }
        check(file.menus.isNotEmpty()) { "メニューが入っていません" }
        check(file.menus.map { it.id }.toSet().size == file.menus.size) { "メニューの id が重複しています" }
        check(file.sessions.map { it.id }.toSet().size == file.sessions.size) { "練習の id が重複しています" }

        val rules = file.menus.associate { m ->
            val rule = try {
                PracticeRules.of(m.ruleType)
            } catch (e: IllegalArgumentException) {
                throw BackupFormatException("対応していないメニューです(${m.ruleType})")
            }
            m.id to rule
        }

        val sessions = file.sessions.map { s ->
            val where = "練習 id ${s.id}"
            val rule = rules[s.menuId] ?: throw BackupFormatException("$where: メニュー id ${s.menuId} がありません")
            val sorted = s.throws.sortedBy { it.seq }
            check(sorted.map { it.seq } == (1..sorted.size).toList()) { "$where: 投の通し番号が 1 から順に並んでいません" }

            // ルール判定に通して作り直す
            val state = try {
                rule.replay(sorted.map { it.rawMarks })
            } catch (e: IllegalArgumentException) {
                throw BackupFormatException("$where: ${e.message}")
            }
            state.throws.zip(sorted).forEach { (expected, actual) ->
                check(expected.target == actual.target) {
                    "$where: ${actual.seq}投目のナンバーがルールと合いません(${actual.target})"
                }
            }
            check(state.finished == s.finished) { "$where: 完了したかどうかが投の記録と合いません" }
            val finishedAt = s.finishedAt?.let { parseTime(it, where) }
            check(!s.finished || finishedAt != null) { "$where: 完了した練習に finishedAt がありません" }

            BackupSession(
                session = SessionEntity(
                    id = s.id,
                    menuId = s.menuId,
                    startedAt = parseTime(s.startedAt, where),
                    finishedAt = if (s.finished) finishedAt else null,
                    finished = s.finished,
                    memo = s.memo,
                ),
                throws = state.throws.map {
                    DartThrowEntity(sessionId = s.id, seq = it.seq, target = it.target, rawMarks = it.rawMarks, marks = it.marks)
                },
            )
        }

        return BackupData(
            menus = file.menus.map { MenuEntity(it.id, it.name, it.ruleType) },
            sessions = sessions,
            exportedAt = file.exportedAt?.let { parseTime(it, "書き出し日時") },
        )
    }

    private fun parseTime(text: String, where: String): Long = try {
        OffsetDateTime.parse(text, dateTime).toInstant().toEpochMilli()
    } catch (e: DateTimeParseException) {
        throw BackupFormatException("$where: 日時の形式が正しくありません($text)")
    }

    private inline fun check(ok: Boolean, message: () -> String) {
        if (!ok) throw BackupFormatException(message())
    }
}
