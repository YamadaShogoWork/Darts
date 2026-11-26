package com.dartspractice.app.domain.rule

/** ruleType からルールを選ぶ。メニューの種類を足すときは、ここに1行足す。 */
object PracticeRules {
    /** 用意しているルール(この順でホームのメニューに並ぶ)。 */
    val all: List<PracticeRule> = listOf(
        Cricket10Rule,
        Bull10Rule,
    )

    fun of(ruleType: String): PracticeRule =
        all.firstOrNull { it.ruleType == ruleType }
            ?: throw IllegalArgumentException("未対応のルールです: $ruleType")
}
