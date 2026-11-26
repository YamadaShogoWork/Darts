package com.dartspractice.app.domain.rule

/**
 * 練習メニューのルール。メニューを増やすときは、この実装を1つ足す。
 *
 * 状態は「入力されたマーク数の列」から毎回計算し直す([replay])。
 * 取り消しは列の最後を外して計算し直すだけでよく、ナンバーの完了やターンも自然に元に戻る。
 */
interface PracticeRule {
    /** Menu.ruleType に入れる値(例: "cricket10")。 */
    val ruleType: String

    /** 対象ナンバーと順番。DartThrow.target に入る値。 */
    val targets: List<String>

    /** 1回の練習で埋める合計マーク数(MPRの計算に使う)。 */
    val totalMarks: Int

    /** 1投で入力できる最大マーク数。 */
    fun maxMarks(target: String): Int

    /**
     * 入力されたマーク数の列から状態を出す。
     * @throws IllegalArgumentException 範囲外のマーク数、または練習終了後の入力があるとき
     */
    fun replay(rawMarks: List<Int>): PracticeState
}

/** 1投の記録。DartThrow に保存する内容と同じ。 */
data class ThrowRecord(
    /** 練習内の通し番号(1から)。 */
    val seq: Int,
    val target: String,
    /** 入力されたマーク数。 */
    val rawMarks: Int,
    /** 実際に加算したマーク数(超過分を切り捨てた値)。 */
    val marks: Int,
) {
    /** ターン番号(1から)。3投で1ターン。 */
    val turn: Int get() = (seq - 1) / 3 + 1
}
