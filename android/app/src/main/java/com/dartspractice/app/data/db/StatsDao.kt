package com.dartspractice.app.data.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 完了した練習ごとの本数と日付。各クエリの土台。 */
private const val SUMMARIES = """
            SELECT s.id AS id, s.startedAt AS startedAt, s.finishedAt AS finishedAt, s.memo AS memo,
                   COUNT(t.id) AS darts,
                   date(s.finishedAt / 1000, 'unixepoch', 'localtime') AS day
            FROM session s JOIN dart_throw t ON t.sessionId = s.id
            WHERE s.menuId = :menuId AND s.finished = 1 AND s.finishedAt IS NOT NULL
            GROUP BY s.id
        """

/** 完了した練習1回分の集計。day は finishedAt を端末の時刻に直した日付("yyyy-MM-dd")。 */
data class SessionSummary(
    val id: Long,
    val startedAt: Long,
    val finishedAt: Long,
    val memo: String,
    val darts: Int,
    val day: String,
)

/** 1日分の集計。 */
data class DailyStat(
    val day: String,
    val sessions: Int,
    val avgDarts: Double,
    val minDarts: Int,
)

/** ナンバー別の本数。avgDarts は練習1回あたりの平均本数。 */
data class TargetStat(
    val target: String,
    val darts: Int,
    val sessions: Int,
) {
    val avgDarts: Double get() = if (sessions == 0) 0.0 else darts.toDouble() / sessions
}

/**
 * 集計用のクエリ。対象は完了した練習だけで、日付は finishedAt で数える。
 * 本数は DartThrow の件数から求め、集計結果は保存しない。
 */
@Dao
interface StatsDao {

    @Query("$SUMMARIES ORDER BY s.finishedAt")
    fun summaries(menuId: Long): Flow<List<SessionSummary>>

    @Query(
        """
        SELECT day, COUNT(*) AS sessions, AVG(darts) AS avgDarts, MIN(darts) AS minDarts
        FROM ($SUMMARIES)
        GROUP BY day ORDER BY day
        """,
    )
    fun daily(menuId: Long): Flow<List<DailyStat>>

    /** 自己ベスト(本数が最少。同じなら先に出した方)。 */
    @Query("SELECT * FROM ($SUMMARIES) ORDER BY darts, finishedAt LIMIT 1")
    fun best(menuId: Long): Flow<SessionSummary?>

    @Query("SELECT * FROM ($SUMMARIES) WHERE day = :day ORDER BY finishedAt")
    fun sessionsOfDay(menuId: Long, day: String): Flow<List<SessionSummary>>

    /** その日の完了した練習の投(練習ごと・投げた順)。 */
    @Query(
        """
        SELECT t.* FROM dart_throw t JOIN ($SUMMARIES) x ON x.id = t.sessionId
        WHERE x.day = :day ORDER BY t.sessionId, t.seq
        """,
    )
    fun throwsOfDay(menuId: Long, day: String): Flow<List<DartThrowEntity>>

    /** fromDay("yyyy-MM-dd")以降に終わった練習の、ナンバー別の本数。 */
    @Query(
        """
        SELECT t.target AS target, COUNT(t.id) AS darts, COUNT(DISTINCT t.sessionId) AS sessions
        FROM dart_throw t JOIN ($SUMMARIES) x ON x.id = t.sessionId
        WHERE x.day >= :fromDay
        GROUP BY t.target
        """,
    )
    fun targetStats(menuId: Long, fromDay: String): Flow<List<TargetStat>>

    /** 指定した練習より前に終わった、完了した練習のうち最後のもの。 */
    @Query("SELECT * FROM ($SUMMARIES) WHERE finishedAt < :finishedAt ORDER BY finishedAt DESC LIMIT 1")
    suspend fun previous(menuId: Long, finishedAt: Long): SessionSummary?
}
