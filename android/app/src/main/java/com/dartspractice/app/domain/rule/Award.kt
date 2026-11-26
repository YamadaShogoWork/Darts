package com.dartspractice.app.domain.rule

import com.dartspractice.app.domain.rule.Cricket10Rule.BULL

/**
 * 1ターンごとのアワード。並びは優先順(上ほど優先)。
 * tier は演出の段階(1〜4)。マーク数は実際に入った数(rawMarks)で数える。
 */
enum class Award(val label: String, val tier: Int, val description: String) {
    ThreeInABed("THREE IN A BED", 4, "3本とも同じナンバーのトリプル"),
    ThreeInTheBlack("THREE IN THE BLACK", 4, "3本ともダブルブル"),
    Marks9("9 MARKS", 3, "1ターンで9マーク"),
    HatTrick("HAT TRICK", 3, "3本ともブル(シングル・ダブルどちらでも)"),
    Marks8("8 MARKS", 3, "1ターンで8マーク"),
    Marks7("7 MARKS", 3, "1ターンで7マーク"),
    Marks6("6 MARKS", 2, "1ターンで6マーク"),
    Marks5("5 MARKS", 2, "1ターンで5マーク"),
    Marks4("4 MARKS", 1, "1ターンで4マーク"),
    Marks3("3 MARKS", 1, "1ターンで3マーク"),
    ;

    companion object {
        private val byMarks = mapOf(9 to Marks9, 8 to Marks8, 7 to Marks7, 6 to Marks6, 5 to Marks5, 4 to Marks4, 3 to Marks3)

        /**
         * 1ターン分の投(1〜3投、投げた順)から、ON になっている中で最上位のアワードを返す。
         * 当てはまるものがなければ null。
         */
        fun judge(turn: List<ThrowRecord>, enabled: Set<Award> = entries.toSet()): Award? {
            if (turn.isEmpty()) return null
            val three = turn.size == 3
            val allBull = turn.all { it.target == BULL }
            val matched = buildSet {
                if (three && turn.all { it.rawMarks == 3 } && turn.map { it.target }.toSet().size == 1) add(ThreeInABed)
                if (three && allBull && turn.all { it.rawMarks == 2 }) add(ThreeInTheBlack)
                if (three && allBull && turn.all { it.rawMarks >= 1 }) add(HatTrick)
                byMarks[turn.sumOf { it.rawMarks }]?.let(::add)
            }
            return entries.firstOrNull { it in matched && it in enabled }
        }

        /**
         * 1投入れたあとの状態から、ちょうど終わったターンの投を返す。
         * ターンの途中なら空。最後のターン(ブル完了)は3投未満でも返す。
         */
        fun finishedTurn(state: PracticeState): List<ThrowRecord> {
            val throws = state.throws
            if (throws.isEmpty()) return emptyList()
            if (throws.size % 3 != 0 && !state.finished) return emptyList()
            val turn = throws.last().turn
            return throws.filter { it.turn == turn }
        }
    }
}
