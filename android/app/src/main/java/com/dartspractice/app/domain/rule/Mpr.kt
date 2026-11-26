package com.dartspractice.app.domain.rule

import java.math.BigDecimal
import java.math.RoundingMode

/** MPR(1ターンあたりの平均マーク数) = 合計マーク数 ÷ 本数 × 3。 */
object Mpr {
    /** 本数が0以下なら null。本数は日ごとの平均(小数)も渡せる。 */
    fun calc(totalMarks: Int, darts: Double): Double? =
        if (darts <= 0.0) null else totalMarks / darts * 3

    /** 小数第2位まで(四捨五入)の表示。値がなければ「—」。 */
    fun format(mpr: Double?): String =
        mpr?.let { BigDecimal(it).setScale(2, RoundingMode.HALF_UP).toPlainString() } ?: "—"
}
