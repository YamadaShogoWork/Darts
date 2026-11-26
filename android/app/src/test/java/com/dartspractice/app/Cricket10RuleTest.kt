package com.dartspractice.app

import com.dartspractice.app.domain.rule.Cricket10Rule
import com.dartspractice.app.domain.rule.Cricket10Rule.BULL
import com.dartspractice.app.domain.rule.Mpr
import com.dartspractice.app.domain.rule.PracticeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class Cricket10RuleTest {

    private val rule = Cricket10Rule
    private fun play(vararg raw: Int): PracticeState = rule.replay(raw.toList())

    /** 20〜15を各4本(3,3,3,1)、ブルを5本(2,2,2,2,2)で埋める最短の入力。 */
    private val fastest = List(6) { listOf(3, 3, 3, 1) }.flatten() + List(5) { 2 }

    @Test
    fun `20に3,3,3,1で4投目に完了し次は19`() {
        val s = play(3, 3, 3)
        assertEquals("20", s.currentTarget)
        assertEquals(9, s.currentMarks)

        val s2 = s.add(1)
        assertEquals(10, s2.marksByTarget["20"])
        assertEquals("19", s2.currentTarget)
        assertEquals(0, s2.currentMarks)
        assertEquals(4, s2.dartsByTarget["20"])
    }

    @Test
    fun `9マークで3を入力すると超過を切り捨てて完了`() {
        val s = play(3, 3, 3, 3)
        val last = s.throws.last()
        assertEquals(3, last.rawMarks)
        assertEquals(1, last.marks)
        assertEquals(10, s.marksByTarget["20"])
        assertEquals("19", s.currentTarget)
    }

    @Test
    fun `ターンの1投目で完了したら残りの2投は次のナンバーの記録`() {
        // 1ターン目 3,3,3 / 2ターン目の1投目で20完了 → 2・3投目は19
        val s = play(3, 3, 3, 1, 2, 3)
        assertEquals(listOf("20", "19", "19"), s.throws.drop(3).map { it.target })
        assertEquals(listOf(2, 2, 2), s.throws.drop(3).map { it.turn })
        assertEquals(5, s.marksByTarget["19"])
    }

    @Test
    fun `ブルは0から2マークだけ入力できる`() {
        assertEquals(3, rule.maxMarks("20"))
        assertEquals(2, rule.maxMarks(BULL))

        val beforeBull = play(*fastest.dropLast(5).toIntArray())
        assertEquals(BULL, beforeBull.currentTarget)
        assertThrows(IllegalArgumentException::class.java) { beforeBull.add(3) }
        assertEquals(2, beforeBull.add(2).throws.last().marks)
    }

    @Test
    fun `範囲外のマークは入力できない`() {
        assertThrows(IllegalArgumentException::class.java) { play(4) }
        assertThrows(IllegalArgumentException::class.java) { play(-1) }
    }

    @Test
    fun `ブルが完了したら練習終了でそれ以上は入力できない`() {
        val s = play(*fastest.toIntArray())
        assertTrue(s.finished)
        assertNull(s.currentTarget)
        assertThrows(IllegalArgumentException::class.java) { s.add(0) }

        assertFalse(s.undo().finished)
    }

    @Test
    fun `取り消すと累計・ナンバー・ターンが元に戻り完了も戻る`() {
        val before = play(3, 3, 3)          // 20が9マーク、2ターン目の1投目を待つ
        val after = before.add(3)           // 20完了、19へ
        assertEquals("19", after.currentTarget)

        val undone = after.undo()
        assertEquals(before, undone)
        assertEquals("20", undone.currentTarget)
        assertEquals(9, undone.currentMarks)
        assertEquals(2, undone.currentTurn)
        assertEquals(1, undone.dartInTurn)
    }

    @Test
    fun `1投もないときの取り消しは何もしない`() {
        val empty = play()
        assertEquals(empty, empty.undo())
        assertEquals("20", empty.currentTarget)
        assertEquals(1, empty.currentTurn)
        assertEquals(1, empty.dartInTurn)
    }

    @Test
    fun `ターンの数え方は3投で1ターン`() {
        val s = play(0, 0, 0, 0)
        assertEquals(listOf(1, 1, 1, 2), s.throws.map { it.turn })
        assertEquals(listOf(1, 2, 3, 4), s.throws.map { it.seq })
        assertEquals(2, s.currentTurn)
        assertEquals(2, s.dartInTurn)
    }

    @Test
    fun `最短は29本でMPRは7_24`() {
        val s = play(*fastest.toIntArray())
        assertEquals(29, s.totalDarts)
        assertEquals(4, s.dartsByTarget["15"])
        assertEquals(5, s.dartsByTarget[BULL])
        assertEquals(70, rule.totalMarks)
        assertEquals("7.24", Mpr.format(Mpr.calc(rule.totalMarks, s.totalDarts.toDouble())))
    }

    @Test
    fun `ミスも本数に数えナンバー別本数の合計は全体と一致する`() {
        val s = play(0, 3, 0, 3, 3, 1, 0, 0, 2)
        assertEquals(6, s.dartsByTarget["20"])
        assertEquals(3, s.dartsByTarget["19"])
        assertEquals(s.totalDarts, s.dartsByTarget.values.sum())
        assertEquals(s.marksByTarget.values.sum(), s.throws.sumOf { it.marks })
    }

    @Test
    fun `MPRの計算と表示`() {
        assertEquals("4.29", Mpr.format(Mpr.calc(70, 49.0)))
        assertEquals("4.47", Mpr.format(Mpr.calc(70, 47.0)))
        assertNull(Mpr.calc(70, 0.0))
        assertEquals("—", Mpr.format(null))
    }
}
