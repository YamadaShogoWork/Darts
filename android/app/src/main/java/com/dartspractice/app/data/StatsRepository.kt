package com.dartspractice.app.data

import com.dartspractice.app.data.db.DailyStat
import com.dartspractice.app.data.db.DataTotals
import com.dartspractice.app.data.db.DartThrowEntity
import com.dartspractice.app.data.db.DartsDatabase
import com.dartspractice.app.data.db.SessionSummary
import com.dartspractice.app.data.db.TargetStat
import kotlinx.coroutines.flow.Flow

/** 集計を読む。DBが変わると Flow が新しい値を出すので、画面は自動で更新される。 */
class StatsRepository(db: DartsDatabase) {
    private val dao = db.statsDao()

    fun daily(menuId: Long): Flow<List<DailyStat>> = dao.daily(menuId)
    fun best(menuId: Long): Flow<SessionSummary?> = dao.best(menuId)
    fun sessionsOfDay(menuId: Long, day: String): Flow<List<SessionSummary>> = dao.sessionsOfDay(menuId, day)
    fun throwsOfDay(menuId: Long, day: String): Flow<List<DartThrowEntity>> = dao.throwsOfDay(menuId, day)
    fun totals(): Flow<DataTotals> = dao.totals()
    fun targetStats(menuId: Long, fromDay: String): Flow<List<TargetStat>> = dao.targetStats(menuId, fromDay)
    suspend fun previous(menuId: Long, finishedAt: Long): SessionSummary? = dao.previous(menuId, finishedAt)
}
