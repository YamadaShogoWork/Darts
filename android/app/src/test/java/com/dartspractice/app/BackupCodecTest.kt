package com.dartspractice.app

import com.dartspractice.app.data.backup.BackupCodec
import com.dartspractice.app.data.backup.BackupData
import com.dartspractice.app.data.backup.BackupFormatException
import com.dartspractice.app.data.backup.BackupSession
import com.dartspractice.app.data.db.DartThrowEntity
import com.dartspractice.app.data.db.MenuEntity
import com.dartspractice.app.data.db.SessionEntity
import com.dartspractice.app.domain.rule.Cricket10Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class BackupCodecTest {

    private val zone = ZoneId.of("Asia/Tokyo")
    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private val fastest = List(6) { listOf(3, 3, 3, 1) }.flatten() + List(5) { 2 }

    private fun session(id: Long, raw: List<Int>, start: String, finish: String?, memo: String = ""): BackupSession {
        val state = Cricket10Rule.replay(raw)
        return BackupSession(
            SessionEntity(id, 1, at(start), finish?.let(::at), finish != null, memo),
            state.throws.map { DartThrowEntity(0, id, it.seq, it.target, it.rawMarks, it.marks) },
        )
    }

    private val data = BackupData(
        menus = listOf(MenuEntity(1, "クリケット10マーク", "cricket10")),
        sessions = listOf(
            session(1, listOf(0, 0) + fastest, "2026-11-26T19:42:00", "2026-11-26T19:51:30", memo = "ブルが入らない"),
            session(2, listOf(3, 3, 3, 3, 1), "2026-11-26T20:30:00", null), // 途中の練習
        ),
    )

    private fun encode() = BackupCodec.encode(data, at("2026-11-26T21:00:00"), zone)

    @Test
    fun 書き出して読み込むと同じ内容に戻る() {
        val back = BackupCodec.decode(encode())
        assertEquals(data.menus, back.menus)
        assertEquals(data.sessions, back.sessions)
        assertEquals(at("2026-11-26T21:00:00"), back.exportedAt)
    }

    @Test
    fun 日時は時差付きで書かれfinishedAtも入る() {
        val text = encode()
        assertTrue(text.contains("\"version\": 1"))
        assertTrue(text.contains("\"startedAt\": \"2026-11-26T19:42:00+09:00\""))
        assertTrue(text.contains("\"finishedAt\": \"2026-11-26T19:51:30+09:00\""))
        assertTrue(text.contains("\"finishedAt\": null"))
        assertTrue(text.contains("\"rawMarks\": 3,\n                    \"marks\": 1") || text.contains("\"marks\": 1"))
    }

    @Test
    fun SPECの例の形を読める() {
        // SPEC.md の例(exportedAt なし)に、完了した練習の finishedAt を足したもの
        val throws = fastest.mapIndexed { i, r ->
            val t = Cricket10Rule.replay(fastest.take(i + 1)).throws.last()
            """{ "seq": ${i + 1}, "target": "${t.target}", "rawMarks": $r, "marks": ${t.marks} }"""
        }.joinToString(",")
        val text = """
            {
              "version": 1,
              "menus": [ { "id": 1, "name": "クリケット10マーク", "ruleType": "cricket10" } ],
              "sessions": [ {
                "id": 1, "menuId": 1,
                "startedAt": "2026-11-26T19:42:00+09:00",
                "finishedAt": "2026-11-26T19:50:00+09:00",
                "finished": true, "memo": "",
                "throws": [ $throws ]
              } ]
            }
        """.trimIndent()
        val back = BackupCodec.decode(text)
        assertNull(back.exportedAt)
        assertEquals(29, back.sessions.single().throws.size)
        assertEquals(at("2026-11-26T19:50:00"), back.sessions.single().session.finishedAt)
    }

    @Test
    fun 超過のmarksはルールで作り直す() {
        // 4投目(9マークの状態で3)の marks を 3 と書き換えても、1 に直して読む
        val text = encode().replaceFirst(
            Regex("(\"seq\": 6,\\s*\"target\": \"20\",\\s*\"rawMarks\": 3,\\s*\"marks\": )1"), "$13",
        )
        val back = BackupCodec.decode(text)
        assertEquals(1, back.sessions[0].throws[5].marks)
    }

    private fun assertRejected(text: String, part: String) {
        val e = assertThrows(BackupFormatException::class.java) { BackupCodec.decode(text) }
        assertTrue("「${e.message}」に「$part」が含まれない", e.message!!.contains(part))
    }

    @Test
    fun 読めないファイルは理由付きで断る() {
        assertRejected("こんにちは", "形式が壊れています")
        assertRejected("{}", "形式が壊れています")
        assertRejected(encode().replace("\"version\": 1", "\"version\": 2"), "対応していない版")
        assertRejected(encode().replace("cricket10", "unknown"), "対応していないメニュー")
        assertRejected(encode().replace("\"menuId\": 1,", "\"menuId\": 9,"), "メニュー id 9")
        assertRejected(encode().replace("+09:00", "JST"), "日時の形式")
    }

    @Test
    fun ルールに合わない投は断る() {
        // ブルに3マーク(ブルは0〜2)
        val bull3 = encode().replace(
            Regex("(\"target\": \"BULL\",\\s*\"rawMarks\": )2"), "$13",
        )
        assertRejected(bull3, "BULL に 3 マークは入力できません")

        // 20が完了する前に19へ
        val wrongTarget = encode().replaceFirst("\"target\": \"20\"", "\"target\": \"19\"")
        assertRejected(wrongTarget, "ナンバーがルールと合いません")

        // 投が足りないのに完了扱い
        val notFinished = encode().replace("\"finished\": false", "\"finished\": true")
        assertRejected(notFinished, "完了したかどうか")

        // 通し番号の抜け
        val gap = encode().replaceFirst("\"seq\": 2,", "\"seq\": 99,")
        assertRejected(gap, "通し番号")
    }
}
