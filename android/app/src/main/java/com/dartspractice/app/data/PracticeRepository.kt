package com.dartspractice.app.data

import androidx.room.withTransaction
import com.dartspractice.app.data.db.DartThrowEntity
import com.dartspractice.app.data.db.DartsDatabase
import com.dartspractice.app.data.db.MenuEntity
import com.dartspractice.app.data.db.SessionEntity
import com.dartspractice.app.domain.rule.PracticeRule
import com.dartspractice.app.domain.rule.PracticeRules
import com.dartspractice.app.domain.rule.PracticeState

/** 読み込んだ練習。状態は保存済みの投から作り直したもの。 */
data class LoadedPractice(
    val session: SessionEntity,
    val menu: MenuEntity,
    val state: PracticeState,
)

/**
 * 練習の記録をDBに保存する。ルール判定は PracticeRule に任せ、ここでは保存だけを行う。
 * 1投ごとにすぐ保存するので、途中でアプリを閉じても記録は残る。
 */
class PracticeRepository(
    private val db: DartsDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val menus = db.menuDao()
    private val sessions = db.sessionDao()
    private val throwsDao = db.dartThrowDao()

    suspend fun defaultMenu(): MenuEntity =
        menus.first() ?: error("メニューがありません")

    /** 再開できる練習(1投以上記録した未完了の練習)。なければ null。 */
    suspend fun resumable(menuId: Long): SessionEntity? =
        sessions.unfinished(menuId).firstOrNull { throwsDao.count(it.id) > 0 }

    /** 未完了の練習を消して、新しい練習を始める。 */
    suspend fun startNew(menuId: Long): Long = db.withTransaction {
        sessions.unfinished(menuId).forEach { sessions.delete(it.id) }
        sessions.insert(SessionEntity(menuId = menuId, startedAt = now()))
    }

    suspend fun load(sessionId: Long): LoadedPractice {
        val session = sessions.get(sessionId) ?: error("練習が見つかりません: $sessionId")
        val menu = menus.get(session.menuId) ?: error("メニューが見つかりません: ${session.menuId}")
        val rule = PracticeRules.of(menu.ruleType)
        val state = rule.replay(throwsDao.list(sessionId).map { it.rawMarks })
        return LoadedPractice(session, menu, state)
    }

    /** 1投を記録する。最後のナンバーが完了したら練習を完了にする。 */
    suspend fun addThrow(sessionId: Long, rule: PracticeRule, rawMarks: Int): PracticeState =
        db.withTransaction {
            val saved = throwsDao.list(sessionId).map { it.rawMarks }
            val state = rule.replay(saved + rawMarks) // 範囲外・終了後の入力はここで例外
            val t = state.throws.last()
            throwsDao.insert(
                DartThrowEntity(sessionId = sessionId, seq = t.seq, target = t.target, rawMarks = t.rawMarks, marks = t.marks),
            )
            if (state.finished) sessions.setFinished(sessionId, true, now())
            state
        }

    /** 完了した練習を日時を指定してまとめて保存する(サンプルデータ用)。 */
    suspend fun insertCompleted(
        menuId: Long,
        rule: PracticeRule,
        startedAt: Long,
        finishedAt: Long,
        rawMarks: List<Int>,
        memo: String = "",
    ): Long = db.withTransaction {
        val state = rule.replay(rawMarks)
        require(state.finished) { "完了していない練習は保存できません" }
        val id = sessions.insert(
            SessionEntity(menuId = menuId, startedAt = startedAt, finishedAt = finishedAt, finished = true, memo = memo),
        )
        state.throws.forEach { t ->
            throwsDao.insert(DartThrowEntity(sessionId = id, seq = t.seq, target = t.target, rawMarks = t.rawMarks, marks = t.marks))
        }
        id
    }

    /** 練習と投の記録をすべて消す(メニューは残す)。 */
    suspend fun deleteAll() = sessions.deleteAll()

    suspend fun saveMemo(sessionId: Long, memo: String) = sessions.setMemo(sessionId, memo.trim())

    /** 練習を消す。投の記録も一緒に消える。 */
    suspend fun delete(sessionId: Long) = sessions.delete(sessionId)

    /** 直前の1投を取り消す。完了していた練習は未完了に戻す。 */
    suspend fun undo(sessionId: Long, rule: PracticeRule): PracticeState =
        db.withTransaction {
            val saved = throwsDao.list(sessionId)
            if (saved.isNotEmpty()) {
                throwsDao.delete(sessionId, saved.last().seq)
                sessions.setFinished(sessionId, false, null)
            }
            rule.replay(saved.dropLast(1).map { it.rawMarks })
        }
}
