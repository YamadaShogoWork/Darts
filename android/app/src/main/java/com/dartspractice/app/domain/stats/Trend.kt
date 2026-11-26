package com.dartspractice.app.domain.stats

import java.time.LocalDate

/** 1日分の平均本数。 */
data class DayPoint(val day: LocalDate, val avgDarts: Double)

/** 期間の最初と最後の日の比較。 */
data class Change(val first: DayPoint, val last: DayPoint) {
    /** 本数の差(マイナスなら減った=上達)。 */
    val diff: Double get() = last.avgDarts - first.avgDarts
}

/** グラフ用の計算。画面から切り離して単体テストする。 */
object Trend {

    /**
     * 移動平均。各点について、その日を含む直近 [windowDays] 日間(カレンダーの日数)のうち
     * 練習した日の平均本数を平均する。点は練習した日にだけ作る。
     * points は日付の古い順。
     */
    fun movingAverage(points: List<DayPoint>, windowDays: Int = 7): List<DayPoint> =
        points.map { p ->
            val from = p.day.minusDays((windowDays - 1).toLong())
            val inWindow = points.filter { it.day in from..p.day }
            DayPoint(p.day, inWindow.map { it.avgDarts }.average())
        }

    /** 今日を含む直近 [days] 日間の点。days が null なら全期間。 */
    fun inPeriod(points: List<DayPoint>, today: LocalDate, days: Int?): List<DayPoint> {
        if (days == null) return points
        val from = today.minusDays((days - 1).toLong())
        return points.filter { it.day in from..today }
    }

    /** 最初の日と最後の日の比較。点がなければ null。 */
    fun change(points: List<DayPoint>): Change? =
        if (points.isEmpty()) null else Change(points.first(), points.last())
}
