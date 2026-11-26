package com.dartspractice.app.domain.rule

/** ある時点での練習の状態。[PracticeRule.replay] で作る。 */
data class PracticeState(
    val rule: PracticeRule,
    val throws: List<ThrowRecord>,
    /** ナンバーごとの累計マーク数(全ナンバーを含む)。 */
    val marksByTarget: Map<String, Int>,
) {
    /** 最後のナンバーまで完了したか。 */
    val finished: Boolean
        get() = rule.targets.all { (marksByTarget[it] ?: 0) >= MARKS_TO_CLEAR }

    /** 今投げるナンバー。終了していれば null。 */
    val currentTarget: String?
        get() = rule.targets.firstOrNull { (marksByTarget[it] ?: 0) < MARKS_TO_CLEAR }

    /** 今のナンバーの累計マーク数。 */
    val currentMarks: Int
        get() = currentTarget?.let { marksByTarget[it] ?: 0 } ?: 0

    /** 合計本数(ミスも数える)。 */
    val totalDarts: Int get() = throws.size

    /** 次に投げるダートのターン番号(1から)。 */
    val currentTurn: Int get() = throws.size / 3 + 1

    /** 次に投げるダートがターンの何投目か(1〜3)。 */
    val dartInTurn: Int get() = throws.size % 3 + 1

    /** ナンバーごとの本数(投げていないナンバーは0)。 */
    val dartsByTarget: Map<String, Int>
        get() = rule.targets.associateWith { t -> throws.count { it.target == t } }

    /** 1投足した状態。 */
    fun add(rawMarks: Int): PracticeState = rule.replay(throws.map { it.rawMarks } + rawMarks)

    /** 直前の1投を取り消した状態。1投もなければそのまま。 */
    fun undo(): PracticeState =
        if (throws.isEmpty()) this else rule.replay(throws.dropLast(1).map { it.rawMarks })

    companion object {
        const val MARKS_TO_CLEAR = 10
    }
}
