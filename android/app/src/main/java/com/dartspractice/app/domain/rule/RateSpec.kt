package com.dartspractice.app.domain.rule

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 本数から求める指標(クリケットは MPR、ブル10本はブル率)。
 * 日ごと・期間の値は、その日(期間)の平均本数から同じ式で求める。
 */
class RateSpec(
    /** 短い名前(例: "MPR")。 */
    val label: String,
    /** 結果画面に出す説明付きの名前(例: "MPR(1ターンの平均マーク数)")。 */
    val longLabel: String,
    /** グラフの縦軸の目盛りの間隔。 */
    val axisStep: Double,
    private val decimals: Int,
    private val suffix: String,
    private val calc: (darts: Double) -> Double,
) {
    /** 本数(平均でもよい)から値を出す。本数が0以下なら null。 */
    fun of(darts: Double): Double? = if (darts <= 0.0) null else calc(darts)

    /** 表示用。値がなければ「—」。 */
    fun format(value: Double?): String =
        value?.let { BigDecimal(it).setScale(decimals, RoundingMode.HALF_UP).toPlainString() + suffix } ?: "—"

    /** グラフの縦軸の目盛りの書き方。 */
    fun axis(value: Double): String =
        BigDecimal(value).setScale(if (axisStep < 1) 1 else 0, RoundingMode.HALF_UP).toPlainString() + suffix

    companion object {
        /** MPR(1ターンあたりの平均マーク数) = 合計マーク数 ÷ 本数 × 3。 */
        fun mpr(totalMarks: Int) = RateSpec(
            label = "MPR",
            longLabel = "MPR(1ターンの平均マーク数)",
            axisStep = 0.5,
            decimals = 2,
            suffix = "",
        ) { darts -> Mpr.calc(totalMarks, darts)!! }

        /** 入った割合(%) = 入れる数 ÷ 本数 × 100。 */
        fun hitRate(label: String, hits: Int) = RateSpec(
            label = label,
            longLabel = "$label(入った割合)",
            axisStep = 5.0,
            decimals = 1,
            suffix = "%",
        ) { darts -> hits / darts * 100 }
    }
}
