package com.dartspractice.app.domain.rule

/**
 * クリケット10マーク。20→19→18→17→16→15→ブルの順に、各10マーク埋める。
 * - シングル1・ダブル2・トリプル3。ブルはシングル1・ダブル2
 * - 10マークを超えた分は切り捨てる
 * - ターンの途中で完了したら、残りのダートは次のナンバーに投げる
 * - ブルが完了したら終了
 */
object Cricket10Rule : PracticeRule {
    const val BULL = "BULL"
    private const val MARKS = 10

    override val ruleType = "cricket10"
    override val defaultName = "クリケット10マーク"
    override val description = "20→15・ブルを各10マークずつ埋める"
    override val targets = listOf("20", "19", "18", "17", "16", "15", BULL)
    override val progressLabel = "マーク"

    /** 1回の練習で埋める合計マーク数(7ナンバー×10 = 70)。 */
    val totalMarks = targets.size * MARKS
    override val rate = RateSpec.mpr(totalMarks)

    override fun marksToClear(target: String) = MARKS
    override fun maxMarks(target: String): Int = if (target == BULL) 2 else 3

    override fun replay(rawMarks: List<Int>): PracticeState =
        replaySequential(this, rawMarks) { _, raw, current -> minOf(raw, MARKS - current) }
}

/**
 * ナンバーを順番に埋めていくルールの共通の計算。
 * added(target, raw, current) で、その1投で累計に加える数を決める。
 */
internal fun replaySequential(
    rule: PracticeRule,
    rawMarks: List<Int>,
    added: (target: String, raw: Int, current: Int) -> Int,
): PracticeState {
    val targets = rule.targets
    val marks = targets.associateWith { 0 }.toMutableMap()
    val throws = ArrayList<ThrowRecord>(rawMarks.size)
    var index = 0 // 今のナンバーの位置

    rawMarks.forEachIndexed { i, raw ->
        require(index < targets.size) { "練習は終了しています(${i + 1}投目)" }
        val target = targets[index]
        require(raw in 0..rule.maxMarks(target)) { "$target に $raw マークは入力できません" }

        val add = added(target, raw, marks.getValue(target))
        marks[target] = marks.getValue(target) + add
        throws += ThrowRecord(seq = i + 1, target = target, rawMarks = raw, marks = add)
        if (marks.getValue(target) >= rule.marksToClear(target)) index++
    }
    return PracticeState(rule = rule, throws = throws, marksByTarget = marks)
}
