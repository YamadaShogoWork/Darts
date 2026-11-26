package com.dartspractice.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dartspractice.app.data.PracticeRepository
import com.dartspractice.app.data.db.DartsDatabase
import com.dartspractice.app.domain.rule.Cricket10Rule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Room への保存・取り消し・完了・再開を実機(またはエミュレータ)で確かめる。 */
@RunWith(AndroidJUnit4::class)
class PracticeRepositoryTest {

    private lateinit var db: DartsDatabase
    private lateinit var repo: PracticeRepository
    private var clock = 1_000L
    private val rule = Cricket10Rule

    /** 20〜15を各4本、ブルを5本で埋める最短の入力。 */
    private val fastest = List(6) { listOf(3, 3, 3, 1) }.flatten() + List(5) { 2 }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DartsDatabase::class.java)
            .addCallback(DartsDatabase.seedCallback)
            .build()
        repo = PracticeRepository(db) { clock }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun 最初のメニューが入っている() = runBlocking {
        val menu = repo.defaultMenu()
        assertEquals(DartsDatabase.DEFAULT_MENU_ID, menu.id)
        assertEquals("cricket10", menu.ruleType)
    }

    @Test
    fun 投げた記録が保存され読み込むと同じ状態になる() = runBlocking {
        val id = repo.startNew(repo.defaultMenu().id)
        listOf(3, 3, 3, 3, 2).forEach { repo.addThrow(id, rule, it) }

        val loaded = repo.load(id)
        assertEquals(5, loaded.state.totalDarts)
        assertEquals("19", loaded.state.currentTarget)
        assertEquals(2, loaded.state.currentMarks)

        val saved = db.dartThrowDao().list(id)
        assertEquals(listOf(1, 2, 3, 4, 5), saved.map { it.seq })
        assertEquals(listOf("20", "20", "20", "20", "19"), saved.map { it.target })
        assertEquals(3, saved[3].rawMarks)
        assertEquals(1, saved[3].marks) // 超過分は切り捨て
    }

    @Test
    fun 取り消すと最後の1投がDBから消える() = runBlocking {
        val id = repo.startNew(repo.defaultMenu().id)
        listOf(3, 3, 3, 1).forEach { repo.addThrow(id, rule, it) }

        val state = repo.undo(id, rule)
        assertEquals("20", state.currentTarget)
        assertEquals(9, state.currentMarks)
        assertEquals(3, db.dartThrowDao().count(id))
        assertEquals(state, repo.load(id).state)
    }

    @Test
    fun ブルが完了すると練習が完了になり終了日時が入る() = runBlocking {
        val id = repo.startNew(repo.defaultMenu().id)
        fastest.dropLast(1).forEach { repo.addThrow(id, rule, it) }
        assertFalse(db.sessionDao().get(id)!!.finished)

        clock = 5_000L
        val state = repo.addThrow(id, rule, 2)
        assertTrue(state.finished)
        val session = db.sessionDao().get(id)!!
        assertTrue(session.finished)
        assertEquals(5_000L, session.finishedAt)

        // 終了後の入力は保存しない
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repo.addThrow(id, rule, 0) } }
        assertEquals(29, db.dartThrowDao().count(id))
    }

    @Test
    fun 完了後に取り消すと未完了に戻る() = runBlocking {
        val id = repo.startNew(repo.defaultMenu().id)
        fastest.forEach { repo.addThrow(id, rule, it) }

        repo.undo(id, rule)
        val session = db.sessionDao().get(id)!!
        assertFalse(session.finished)
        assertNull(session.finishedAt)
    }

    @Test
    fun 途中の練習は再開でき新しく始めると消える() = runBlocking {
        val menuId = repo.defaultMenu().id
        val empty = repo.startNew(menuId)
        assertNull(repo.resumable(menuId)) // 1投もない練習は再開の対象にしない

        repo.addThrow(empty, rule, 3)
        assertEquals(empty, repo.resumable(menuId)?.id)

        val next = repo.startNew(menuId)
        assertNull(db.sessionDao().get(empty))
        assertEquals(0, db.dartThrowDao().count(empty)) // 投も一緒に消える
        assertNotNull(db.sessionDao().get(next))
    }

    @Test
    fun 完了した練習は再開の対象にならず新しく始めても消えない() = runBlocking {
        val menuId = repo.defaultMenu().id
        val done = repo.startNew(menuId)
        fastest.forEach { repo.addThrow(done, rule, it) }

        assertNull(repo.resumable(menuId))
        repo.startNew(menuId)
        assertNotNull(db.sessionDao().get(done))
    }
}
