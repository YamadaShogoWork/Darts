package com.dartspractice.app

import com.dartspractice.app.domain.rule.Bull10Rule
import com.dartspractice.app.domain.rule.Cricket10Rule
import com.dartspractice.app.domain.rule.PracticeRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class Bull10RuleTest {

    private val rule = Bull10Rule
    private fun play(vararg raw: Int) = rule.replay(raw.toList())

    @Test
    fun ブルに10本入ったら終了() {
        val nine = play(*IntArray(9) { 1 })
        assertFalse(nine.finished)
        assertEquals("BULL", nine.currentTarget)
        assertEquals(9, nine.currentMarks)

        val ten = nine.add(1)
        assertTrue(ten.finished)
        assertNull(ten.currentTarget)
        assertEquals(10, ten.totalDarts) // 最短は10本
        assertThrows(IllegalArgumentException::class.java) { ten.add(0) }
    }

    @Test
    fun ダブルブルも1本と数える() {
        val s = play(2, 2, 1)
        assertEquals(3, s.currentMarks)
        assertEquals(listOf(2, 2, 1), s.throws.map { it.rawMarks }) // シングルかダブルかは記録に残る
        assertEquals(listOf(1, 1, 1), s.throws.map { it.marks })
    }

    @Test
    fun ミスも本数に数える() {
        val raw = listOf(0, 1, 0, 0, 2, 1, 0, 1, 1, 2, 1, 1, 1, 2) // 14本で10本入る
        val s = rule.replay(raw)
        assertTrue(s.finished)
        assertEquals(14, s.totalDarts)
        assertEquals(14, s.dartsByTarget["BULL"])
    }

    @Test
    fun トリプルは入力できない() {
        assertEquals(2, rule.maxMarks("BULL"))
        assertThrows(IllegalArgumentException::class.java) { play(3) }
    }

    @Test
    fun 取り消すと元に戻り完了も戻る() {
        val ten = play(*IntArray(10) { 1 })
        val undone = ten.undo()
        assertFalse(undone.finished)
        assertEquals(9, undone.currentMarks)
        assertEquals(play(*IntArray(9) { 1 }), undone)
    }

    @Test
    fun ブル率は入った割合() {
        assertEquals("ブル率", rule.rate.label)
        assertEquals("40.0%", rule.rate.format(rule.rate.of(25.0)))
        assertEquals("100.0%", rule.rate.format(rule.rate.of(10.0)))
        assertEquals("—", rule.rate.format(rule.rate.of(0.0)))
    }

    @Test
    fun ルールの一覧から選べる() {
        assertEquals(Bull10Rule, PracticeRules.of("bull10"))
        assertEquals(Cricket10Rule, PracticeRules.of("cricket10"))
        assertEquals(listOf("cricket10", "bull10"), PracticeRules.all.map { it.ruleType })
        // クリケットの指標は今までどおり MPR
        assertEquals("4.29", Cricket10Rule.rate.format(Cricket10Rule.rate.of(49.0)))
    }
}
