package com.dartspractice.app.domain.rule

import com.dartspractice.app.domain.rule.PracticeState.Companion.MARKS_TO_CLEAR

/**
 * クリケット10マーク。20→19→18→17→16→15→ブルの順に、各10マーク埋める。
 * - シングル1・ダブル2・トリプル3。ブルはシングル1・ダブル2
 * - 10マークを超えた分は切り捨てる
 * - ターンの途中で完了したら、残りのダートは次のナンバーに投げる
 * - ブルが完了したら終了
 */
object Cricket10Rule : PracticeRule {
    const val BULL = "BULL"

    override val ruleType = "cricket10"
    override val targets = listOf("20", "19", "18", "17", "16", "15", BULL)
    override val totalMarks = targets.size * MARKS_TO_CLEAR

    override fun maxMarks(target: String): Int = if (target == BULL) 2 else 3

    override fun replay(rawMarks: List<Int>): PracticeState {
        val marks = targets.associateWith { 0 }.toMutableMap()
        val throws = ArrayList<ThrowRecord>(rawMarks.size)
        var index = 0 // 今のナンバーの位置

        rawMarks.forEachIndexed { i, raw ->
            require(index < targets.size) { "練習は終了しています(${i + 1}投目)" }
            val target = targets[index]
            require(raw in 0..maxMarks(target)) { "$target に $raw マークは入力できません" }

            val added = minOf(raw, MARKS_TO_CLEAR - marks.getValue(target))
            marks[target] = marks.getValue(target) + added
            throws += ThrowRecord(seq = i + 1, target = target, rawMarks = raw, marks = added)
            if (marks.getValue(target) >= MARKS_TO_CLEAR) index++
        }
        return PracticeState(rule = this, throws = throws, marksByTarget = marks)
    }
}
