package com.dartspractice.app

import com.dartspractice.app.domain.rule.Award
import com.dartspractice.app.domain.rule.Cricket10Rule
import com.dartspractice.app.domain.rule.ThrowRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AwardTest {

    private fun t(target: String, raw: Int, seq: Int = 1) = ThrowRecord(seq, target, raw, raw)
    private fun turn(vararg throws: Pair<String, Int>) = throws.mapIndexed { i, (target, raw) -> t(target, raw, i + 1) }

    /** 20〜15を各4本、ブルを5本で埋める最短の入力。 */
    private val fastest = List(6) { listOf(3, 3, 3, 1) }.flatten() + List(5) { 2 }

    @Test
    fun マーク数のアワードは3から9マーク() {
        assertNull(Award.judge(turn("20" to 1, "20" to 1, "20" to 0)))   // 2マークは出ない
        assertEquals(Award.Marks3, Award.judge(turn("20" to 1, "20" to 1, "20" to 1)))
        assertEquals(Award.Marks4, Award.judge(turn("20" to 3, "20" to 1, "20" to 0)))
        assertEquals(Award.Marks5, Award.judge(turn("20" to 3, "20" to 2, "20" to 0)))
        assertEquals(Award.Marks6, Award.judge(turn("20" to 3, "20" to 3, "20" to 0)))
        assertEquals(Award.Marks7, Award.judge(turn("20" to 3, "20" to 3, "20" to 1)))
        assertEquals(Award.Marks8, Award.judge(turn("20" to 3, "20" to 3, "20" to 2)))
    }

    @Test
    fun 同じナンバーのトリプル3本はTHREE_IN_A_BED() {
        assertEquals(Award.ThreeInABed, Award.judge(turn("20" to 3, "20" to 3, "20" to 3)))
    }

    @Test
    fun ナンバーをまたいだトリプル3本は9マーク() {
        assertEquals(Award.Marks9, Award.judge(turn("20" to 3, "19" to 3, "19" to 3)))
    }

    @Test
    fun ブルのアワード() {
        assertEquals(Award.ThreeInTheBlack, Award.judge(turn("BULL" to 2, "BULL" to 2, "BULL" to 2)))
        assertEquals(Award.HatTrick, Award.judge(turn("BULL" to 1, "BULL" to 2, "BULL" to 1)))   // 4マークより優先
        assertEquals(Award.HatTrick, Award.judge(turn("BULL" to 1, "BULL" to 1, "BULL" to 1)))   // 3マークより優先
        assertEquals(Award.Marks4, Award.judge(turn("BULL" to 2, "BULL" to 0, "BULL" to 2)))     // ミスがあればハットトリックではない
        assertEquals(Award.Marks5, Award.judge(turn("15" to 3, "BULL" to 2, "BULL" to 0)))
    }

    @Test
    fun OFFのアワードは飛ばして次に当てはまるものを出す() {
        val black = turn("BULL" to 2, "BULL" to 2, "BULL" to 2)
        val noBlack = Award.entries.toSet() - Award.ThreeInTheBlack
        assertEquals(Award.HatTrick, Award.judge(black, noBlack))
        assertEquals(Award.Marks6, Award.judge(black, noBlack - Award.HatTrick))
        assertNull(Award.judge(black, emptySet()))
    }

    @Test
    fun 実際に入ったマーク数で数える() {
        // 9マークの状態でトリプル → 記録上は1マークでも、3マークとして数える
        val state = Cricket10Rule.replay(listOf(3, 3, 3, 3, 0, 0))
        val turn2 = Award.finishedTurn(state)
        assertEquals(listOf(1, 0, 0), turn2.map { it.marks })
        assertEquals(Award.Marks3, Award.judge(turn2))
    }

    @Test
    fun 終わったターンの取り出し() {
        assertTrue(Award.finishedTurn(Cricket10Rule.replay(listOf(3, 3))).isEmpty()) // ターンの途中
        assertEquals(3, Award.finishedTurn(Cricket10Rule.replay(listOf(3, 3, 3))).size)
        assertEquals(listOf(4, 5, 6), Award.finishedTurn(Cricket10Rule.replay(listOf(3, 3, 3, 1, 2, 3))).map { it.seq })

        // 最後のターンは3投未満でも返す(29本 = 9ターン + 2投)
        val last = Award.finishedTurn(Cricket10Rule.replay(fastest))
        assertEquals(listOf(28, 29), last.map { it.seq })
        assertEquals(Award.Marks4, Award.judge(last))
    }

    @Test
    fun 段階は4つ() {
        assertEquals(setOf(1, 2, 3, 4), Award.entries.map { it.tier }.toSet())
        assertEquals(10, Award.entries.size)
    }
}
