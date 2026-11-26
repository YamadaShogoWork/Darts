package com.dartspractice.app.domain.rule

import com.dartspractice.app.domain.rule.Cricket10Rule.BULL

/**
 * ブル10本。ブルだけを投げて、ブルに10本入ったら終了。かかった本数を記録する(少ないほど上達。最短10本)。
 * - シングルブル・ダブルブルのどちらも1本と数える(入力は区別して rawMarks に残す)
 * - ミスも本数に数える
 */
object Bull10Rule : PracticeRule {
    private const val HITS = 10

    override val ruleType = "bull10"
    override val defaultName = "ブル10本"
    override val description = "ブルに10本入るまで投げる"
    override val targets = listOf(BULL)
    override val progressLabel = "入った本数"
    override val rate = RateSpec.hitRate("ブル率", HITS)

    override fun marksToClear(target: String) = HITS
    override fun maxMarks(target: String): Int = 2

    override fun replay(rawMarks: List<Int>): PracticeState =
        replaySequential(this, rawMarks) { _, raw, _ -> if (raw > 0) 1 else 0 }
}
