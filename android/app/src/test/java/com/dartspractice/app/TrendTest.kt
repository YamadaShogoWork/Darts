package com.dartspractice.app

import com.dartspractice.app.domain.rule.Mpr
import com.dartspractice.app.domain.stats.DayPoint
import com.dartspractice.app.domain.stats.Trend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TrendTest {

    private val today = LocalDate.of(2026, 11, 26)
    private fun p(daysAgo: Long, avg: Double) = DayPoint(today.minusDays(daysAgo), avg)

    @Test
    fun 移動平均は直近7日間の練習した日を平均する() {
        val points = listOf(p(8, 70.0), p(6, 60.0), p(3, 50.0), p(0, 40.0))
        val ma = Trend.movingAverage(points)

        assertEquals(points.map { it.day }, ma.map { it.day }) // 点は練習した日だけ
        assertEquals(70.0, ma[0].avgDarts, 1e-9)               // 窓に自分だけ
        assertEquals(65.0, ma[1].avgDarts, 1e-9)               // 8日前と6日前
        assertEquals(60.0, ma[2].avgDarts, 1e-9)               // 窓は9日前〜3日前 → 8日前・6日前・3日前
        assertEquals(50.0, ma[3].avgDarts, 1e-9)               // 6日前・3日前・今日
    }

    @Test
    fun 移動平均の窓の境目() {
        // 今日を含めて7日 → 6日前は入り、7日前は入らない
        val ma = Trend.movingAverage(listOf(p(7, 100.0), p(6, 60.0), p(0, 40.0)))
        assertEquals(50.0, ma.last().avgDarts, 1e-9)
    }

    @Test
    fun 期間の切り出し() {
        val points = listOf(p(40, 1.0), p(29, 2.0), p(7, 3.0), p(6, 4.0), p(0, 5.0))
        assertEquals(listOf(4.0, 5.0), Trend.inPeriod(points, today, 7).map { it.avgDarts })
        assertEquals(listOf(2.0, 3.0, 4.0, 5.0), Trend.inPeriod(points, today, 30).map { it.avgDarts })
        assertEquals(points, Trend.inPeriod(points, today, null))
    }

    @Test
    fun 最初と最後の比較() {
        val change = Trend.change(listOf(p(13, 64.0), p(5, 55.0), p(0, 49.0)))!!
        assertEquals(64.0, change.first.avgDarts, 1e-9)
        assertEquals(49.0, change.last.avgDarts, 1e-9)
        assertEquals(-15.0, change.diff, 1e-9)
        assertNull(Trend.change(emptyList()))
    }

    @Test
    fun 平均本数をMPRに直す() {
        assertEquals("3.28", Mpr.format(Mpr.calc(70, 64.0)))
        assertEquals("4.29", Mpr.format(Mpr.calc(70, 49.0)))
    }
}
