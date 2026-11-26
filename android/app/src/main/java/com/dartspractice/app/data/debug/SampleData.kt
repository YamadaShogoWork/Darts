package com.dartspractice.app.data.debug

import com.dartspractice.app.domain.rule.PracticeRule
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/** サンプルの練習1回分。rawMarks はブルまで完了する入力の列。 */
data class SamplePractice(
    val startedAt: Long,
    val finishedAt: Long,
    val rawMarks: List<Int>,
    val memo: String,
)

/**
 * 開発用のサンプルデータ。今日までの14日分の練習を作る(練習しない日も混ぜる)。
 * 1投ずつ乱数で投げ、ルール判定を通して入力の列を作る。日が進むほど上手くなる(本数が減る)。
 * 乱数の種を固定しているので、同じ日に作れば毎回同じ内容になる。
 */
object SampleData {
    const val DAYS = 14
    /** 練習しない日(今日から何日前か)。 */
    private val restDays = setOf(10, 6, 3)
    private val memos = listOf("ブルが入らない。肩の力を抜く", "19がよかった", "16で崩れた", "集中できた", "疲れ気味")

    // マーク数0〜3の出やすさ。最初の日 → 今日 へ少しずつ変える
    private val numberFrom = doubleArrayOf(0.30, 0.35, 0.15, 0.20)
    private val numberTo = doubleArrayOf(0.20, 0.30, 0.20, 0.30)
    private val bullFrom = doubleArrayOf(0.45, 0.40, 0.15)
    private val bullTo = doubleArrayOf(0.30, 0.45, 0.25)

    fun generate(
        rule: PracticeRule,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        seed: Int = 42,
    ): List<SamplePractice> {
        val random = Random(seed)
        val result = mutableListOf<SamplePractice>()
        for (daysAgo in DAYS - 1 downTo 0) {
            if (daysAgo in restDays) continue
            val skill = (DAYS - 1 - daysAgo).toDouble() / (DAYS - 1) // 0.0 → 1.0
            val day = today.minusDays(daysAgo.toLong())
            var start = day.atTime(19, random.nextInt(0, 30)).atZone(zone).toInstant().toEpochMilli()
            repeat(random.nextInt(1, 4)) {
                val raw = play(rule, skill, random)
                val finish = start + raw.size * 12_000L // 1投12秒くらい
                val memo = if (random.nextDouble() < 0.3) memos[random.nextInt(memos.size)] else ""
                result += SamplePractice(start, finish, raw, memo)
                start = finish + random.nextLong(5, 20) * 60_000L
            }
        }
        return result
    }

    /** ブルが完了するまで投げて、入力の列を返す。 */
    private fun play(rule: PracticeRule, skill: Double, random: Random): List<Int> {
        var state = rule.replay(emptyList())
        while (!state.finished) {
            val target = state.currentTarget!!
            val weights = if (rule.maxMarks(target) == 2) mix(bullFrom, bullTo, skill) else mix(numberFrom, numberTo, skill)
            state = state.add(pick(weights, random))
        }
        return state.throws.map { it.rawMarks }
    }

    private fun mix(from: DoubleArray, to: DoubleArray, t: Double) =
        DoubleArray(from.size) { from[it] + (to[it] - from[it]) * t }

    private fun pick(weights: DoubleArray, random: Random): Int {
        var r = random.nextDouble() * weights.sum()
        weights.forEachIndexed { i, w ->
            r -= w
            if (r < 0) return i
        }
        return weights.lastIndex
    }
}
