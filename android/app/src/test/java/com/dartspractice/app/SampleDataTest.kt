package com.dartspractice.app

import com.dartspractice.app.data.debug.SampleData
import com.dartspractice.app.domain.rule.Cricket10Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SampleDataTest {

    private val zone = ZoneId.of("Asia/Tokyo")
    private val today = LocalDate.of(2026, 11, 26)
    private val samples = SampleData.generate(Cricket10Rule, today, zone)

    private fun dayOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    @Test
    fun すべての練習がルールどおりにブルまで完了している() {
        samples.forEach { s ->
            val state = Cricket10Rule.replay(s.rawMarks) // 範囲外の入力があればここで例外
            assertTrue(state.finished)
            assertTrue(state.totalDarts >= 29)
        }
    }

    @Test
    fun 日付は今日までの14日の範囲で休みの日もある() {
        val days = samples.map { dayOf(it.finishedAt) }.toSet()
        assertTrue(days.all { it in today.minusDays(13)..today })
        assertTrue(today in days)
        assertEquals(11, days.size) // 14日のうち3日は休み
        samples.forEach { assertTrue(it.finishedAt > it.startedAt) }
        samples.groupBy { dayOf(it.finishedAt) }.values.forEach { assertTrue(it.size in 1..3) }
    }

    @Test
    fun 何度作っても同じ内容になる() {
        assertEquals(samples, SampleData.generate(Cricket10Rule, today, zone))
    }

    @Test
    fun 日が進むほど本数が減っていく() {
        val byDay = samples.groupBy { dayOf(it.finishedAt) }.toSortedMap()
            .values.map { list -> list.map { it.rawMarks.size }.average() }
        val first = byDay.take(3).average()
        val last = byDay.takeLast(3).average()
        assertTrue("最初 $first → 最後 $last", last < first)
    }
}
