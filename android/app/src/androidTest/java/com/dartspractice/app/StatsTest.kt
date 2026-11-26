package com.dartspractice.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dartspractice.app.data.PracticeRepository
import com.dartspractice.app.data.StatsRepository
import com.dartspractice.app.data.db.DartsDatabase
import com.dartspractice.app.data.debug.SampleData
import com.dartspractice.app.domain.rule.Cricket10Rule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** 集計(日ごと・自己ベスト・前回・日別の内訳)を確かめる。日付は finishedAt で数える。 */
@RunWith(AndroidJUnit4::class)
class StatsTest {

    private lateinit var db: DartsDatabase
    private lateinit var repo: PracticeRepository
    private lateinit var stats: StatsRepository
    private var clock = 0L
    private val menuId = DartsDatabase.DEFAULT_MENU_ID

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DartsDatabase::class.java)
            .addCallback(DartsDatabase.seedCallback)
            .build()
        repo = PracticeRepository(db) { clock }
        stats = StatsRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private fun at(text: String): Long =
        LocalDateTime.parse(text).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /**
     * 練習を1回記録する。最短29本に、20へのミスを misses 本足す(合計 29 + misses 本)。
     * finish が null なら最後の1投を投げずに未完了のままにする。
     */
    private suspend fun practice(start: String, finish: String?, misses: Int = 0): Long {
        clock = at(start)
        val id = repo.startNew(menuId)
        val raw = List(misses) { 0 } + List(6) { listOf(3, 3, 3, 1) }.flatten() + List(5) { 2 }
        raw.dropLast(1).forEach { repo.addThrow(id, Cricket10Rule, it) }
        if (finish != null) {
            clock = at(finish)
            repo.addThrow(id, Cricket10Rule, raw.last())
        }
        return id
    }

    @Test
    fun 日ごとの回数_平均_最少() = runBlocking {
        practice("2026-11-25T19:00", "2026-11-25T19:10", misses = 18) // 47本
        practice("2026-11-25T20:00", "2026-11-25T20:10", misses = 22) // 51本
        practice("2026-11-26T19:00", "2026-11-26T19:10", misses = 21) // 50本

        val daily = stats.daily(menuId).first()
        assertEquals(listOf("2026-11-25", "2026-11-26"), daily.map { it.day })
        assertEquals(2, daily[0].sessions)
        assertEquals(49.0, daily[0].avgDarts, 0.001)
        assertEquals(47, daily[0].minDarts)
        assertEquals(50, daily[1].minDarts)
    }

    @Test
    fun 日付は終了日時で数え日をまたいだ練習は次の日に入る() = runBlocking {
        practice("2026-11-25T23:50", "2026-11-26T00:10")
        assertEquals(listOf("2026-11-26"), stats.daily(menuId).first().map { it.day })
    }

    @Test
    fun 未完了の練習は集計に入らない() = runBlocking {
        practice("2026-11-26T19:00", null, misses = 5)
        assertEquals(emptyList<Any>(), stats.daily(menuId).first())
        assertNull(stats.best(menuId).first())

        practice("2026-11-26T20:00", "2026-11-26T20:10") // 新しく始めると未完了は消える
        assertEquals(1, stats.daily(menuId).first().single().sessions)
    }

    @Test
    fun 自己ベストは本数が最少の練習() = runBlocking {
        practice("2026-11-24T19:00", "2026-11-24T19:10", misses = 10)
        practice("2026-11-25T19:00", "2026-11-25T19:10", misses = 3)
        practice("2026-11-26T19:00", "2026-11-26T19:10", misses = 3) // 同じ本数なら先の方

        val best = stats.best(menuId).first()!!
        assertEquals(32, best.darts)
        assertEquals("2026-11-25", best.day)
    }

    @Test
    fun 前回の練習() = runBlocking {
        practice("2026-11-25T19:00", "2026-11-25T19:10", misses = 21)
        practice("2026-11-26T19:00", "2026-11-26T19:10", misses = 18)

        val prev = stats.previous(menuId, at("2026-11-26T19:10"))!!
        assertEquals(50, prev.darts)
        assertNull(stats.previous(menuId, at("2026-11-25T19:10")))
    }

    @Test
    fun 日別の練習と投の一覧() = runBlocking {
        val a = practice("2026-11-26T19:00", "2026-11-26T19:10", misses = 1)
        val b = practice("2026-11-26T20:00", "2026-11-26T20:10")

        val sessions = stats.sessionsOfDay(menuId, "2026-11-26").first()
        assertEquals(listOf(a, b), sessions.map { it.id })
        assertEquals(listOf(30, 29), sessions.map { it.darts })
        assertEquals(at("2026-11-26T19:00"), sessions[0].startedAt)

        val throws = stats.throwsOfDay(menuId, "2026-11-26").first()
        assertEquals(59, throws.size)
        assertEquals((1..30).toList(), throws.filter { it.sessionId == a }.map { it.seq })
    }

    @Test
    fun サンプルデータを入れると集計に表れ全部消すとゼロに戻る() = runBlocking {
        val samples = SampleData.generate(Cricket10Rule, LocalDate.now())
        samples.forEach { repo.insertCompleted(menuId, Cricket10Rule, it.startedAt, it.finishedAt, it.rawMarks, it.memo) }

        val daily = stats.daily(menuId).first()
        assertEquals(samples.size, daily.sumOf { it.sessions })
        assertEquals(LocalDate.now().toString(), daily.last().day)
        // 今日の練習の本数が、作った入力の数と一致する
        val today = LocalDate.now().toString()
        val todaySamples = samples.filter {
            Instant.ofEpochMilli(it.finishedAt).atZone(ZoneId.systemDefault()).toLocalDate().toString() == today
        }
        assertEquals(
            todaySamples.map { it.rawMarks.size },
            stats.sessionsOfDay(menuId, today).first().map { it.darts },
        )

        repo.deleteAll()
        assertEquals(emptyList<Any>(), stats.daily(menuId).first())
        assertEquals(DartsDatabase.DEFAULT_MENU_ID, repo.defaultMenu().id) // メニューは残る
    }

    @Test
    fun ナンバー別の平均本数は期間内の完了した練習だけで数える() = runBlocking {
        practice("2026-11-19T19:00", "2026-11-19T19:10", misses = 10) // 期間外(20に14本)
        practice("2026-11-20T19:00", "2026-11-20T19:10", misses = 2)  // 期間の初日(20に6本)
        practice("2026-11-26T19:00", "2026-11-26T19:10", misses = 0)  // 20に4本
        practice("2026-11-26T20:00", null, misses = 30)               // 未完了は数えない

        val stats7 = stats.targetStats(menuId, "2026-11-20").first().associateBy { it.target }
        assertEquals(7, stats7.size)
        assertEquals(2, stats7.getValue("20").sessions)
        assertEquals(5.0, stats7.getValue("20").avgDarts, 1e-9) // (6 + 4) / 2
        assertEquals(4.0, stats7.getValue("19").avgDarts, 1e-9)
        assertEquals(5.0, stats7.getValue("BULL").avgDarts, 1e-9)

        val all = stats.targetStats(menuId, "2026-01-01").first().associateBy { it.target }
        assertEquals(8.0, all.getValue("20").avgDarts, 1e-9) // (14 + 6 + 4) / 3
    }

    @Test
    fun メモの保存と破棄() = runBlocking {
        val id = practice("2026-11-26T19:00", "2026-11-26T19:10")
        repo.saveMemo(id, "  ブルが入らない  ")
        assertEquals("ブルが入らない", stats.sessionsOfDay(menuId, "2026-11-26").first().single().memo)

        repo.delete(id)
        assertEquals(emptyList<Any>(), stats.daily(menuId).first())
        assertEquals(0, db.dartThrowDao().count(id))
    }
}
