package com.dartspractice.app.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** 画面に出す日付・時刻・本数の書き方をまとめる。 */
object Format {
    private val dayWeek = DateTimeFormatter.ofPattern("M/d(E)", Locale.JAPANESE)
    private val monthDay = DateTimeFormatter.ofPattern("M/d")
    private val time = DateTimeFormatter.ofPattern("H:mm")
    private val dateTime = DateTimeFormatter.ofPattern("M/d H:mm")

    /** "2026-11-26" → "11/26(木)" */
    fun dayWithWeek(day: String): String = LocalDate.parse(day).format(dayWeek)
    fun dayWithWeek(day: LocalDate): String = day.format(dayWeek)

    /** "2026-11-26" → "11/26" */
    fun monthDay(day: String): String = LocalDate.parse(day).format(monthDay)

    fun time(millis: Long): String = toLocal(millis).format(time)
    fun dateTime(millis: Long): String = toLocal(millis).format(dateTime)

    /** 平均本数。割り切れれば整数、そうでなければ小数第1位まで(49 / 48.5)。 */
    fun darts(avg: Double): String {
        val tenth = (avg * 10).roundToInt()
        return if (tenth % 10 == 0) "${tenth / 10}" else "%.1f".format(Locale.US, tenth / 10.0)
    }

    private fun toLocal(millis: Long) = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
}
