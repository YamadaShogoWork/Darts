package com.dartspractice.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dartspractice.app.data.PracticeRepository
import com.dartspractice.app.data.StatsRepository
import com.dartspractice.app.data.backup.BackupCodec
import com.dartspractice.app.data.backup.BackupRepository
import com.dartspractice.app.data.db.DartsDatabase
import com.dartspractice.app.data.db.MenuEntity
import com.dartspractice.app.domain.rule.Bull10Rule
import com.dartspractice.app.domain.rule.Cricket10Rule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** メニューが2つ(クリケット10マーク・ブル10本)あるときの動きを確かめる。 */
@RunWith(AndroidJUnit4::class)
class MenuTest {

    private lateinit var db: DartsDatabase
    private lateinit var repo: PracticeRepository
    private lateinit var stats: StatsRepository
    private var clock = 1_764_150_000_000L

    private val cricket = List(6) { listOf(3, 3, 3, 1) }.flatten() + List(5) { 2 } // 29本
    private val bull = listOf(0, 1, 2, 0, 1, 1, 2, 1, 0, 1, 1, 2, 1) // 13本で10本入る

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

    private suspend fun menuOf(ruleType: String): MenuEntity = repo.menus().first { it.ruleType == ruleType }

    @Test
    fun ブル10本のメニューが自動で用意され何度呼んでも増えない() = runBlocking {
        val menus = repo.menus()
        assertEquals(listOf("cricket10", "bull10"), menus.map { it.ruleType })
        assertEquals("ブル10本", menus[1].name)
        assertEquals(DartsDatabase.DEFAULT_MENU_ID, menus[0].id) // 今までのメニューはそのまま
        assertEquals(2, repo.menus().size)
    }

    @Test
    fun 集計はメニューごとに分かれる() = runBlocking {
        val c = menuOf("cricket10")
        val b = menuOf("bull10")
        repo.insertCompleted(c.id, Cricket10Rule, clock, clock + 1_000, cricket)
        repo.insertCompleted(b.id, Bull10Rule, clock, clock + 2_000, bull)

        assertEquals(29.0, stats.daily(c.id).first().single().avgDarts, 1e-9)
        assertEquals(13.0, stats.daily(b.id).first().single().avgDarts, 1e-9)
        assertEquals(13, stats.best(b.id).first()!!.darts)
        assertEquals(2, stats.totals().first().sessions) // 設定画面の数は全メニューの合計
    }

    @Test
    fun ブル10本の練習を1投ずつ記録して完了できる() = runBlocking {
        val b = menuOf("bull10")
        val id = repo.startNew(b.id)
        bull.forEach { repo.addThrow(id, Bull10Rule, it) }

        val loaded = repo.load(id)
        assertEquals(Bull10Rule, loaded.state.rule)
        assertEquals(13, loaded.state.totalDarts)
        assertEquals(true, db.sessionDao().get(id)!!.finished)
    }

    @Test
    fun 途中の練習の再開はメニューごと() = runBlocking {
        val c = menuOf("cricket10")
        val b = menuOf("bull10")
        val cId = repo.startNew(c.id)
        repo.addThrow(cId, Cricket10Rule, 3)

        assertNull(repo.resumable(b.id))
        repo.startNew(b.id) // ブル10本を始めても、クリケットの途中の練習は消えない
        assertEquals(cId, repo.resumable(c.id)?.id)
    }

    @Test
    fun メニュー2つ入りのバックアップが元に戻る() = runBlocking {
        val c = menuOf("cricket10")
        val b = menuOf("bull10")
        repo.insertCompleted(c.id, Cricket10Rule, clock, clock + 1_000, cricket)
        repo.insertCompleted(b.id, Bull10Rule, clock, clock + 2_000, bull)
        val backup = BackupRepository(db)
        val json = BackupCodec.encode(backup.exportData(), clock)
        val before = Pair(db.menuDao().all(), db.dartThrowDao().all().map { it.copy(id = 0) })

        repo.deleteAll()
        backup.replaceAll(BackupCodec.decode(json))

        assertEquals(before, Pair(db.menuDao().all(), db.dartThrowDao().all().map { it.copy(id = 0) }))
        assertEquals(13, stats.best(b.id).first()!!.darts)
    }
}
