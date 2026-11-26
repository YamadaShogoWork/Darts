package com.dartspractice.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dartspractice.app.data.PracticeRepository
import com.dartspractice.app.data.backup.BackupCodec
import com.dartspractice.app.data.backup.BackupData
import com.dartspractice.app.data.backup.BackupRepository
import com.dartspractice.app.data.backup.BackupSession
import com.dartspractice.app.data.db.DartThrowEntity
import com.dartspractice.app.data.db.DartsDatabase
import com.dartspractice.app.data.db.SessionEntity
import com.dartspractice.app.domain.rule.Cricket10Rule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** バックアップの書き出し → 読み込み(置き換え)を、実機のDBで確かめる。 */
@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {

    private lateinit var db: DartsDatabase
    private lateinit var repo: PracticeRepository
    private lateinit var backup: BackupRepository
    private var clock = 1_764_150_000_000L // 2025-11-26 頃(秒単位)
    private val menuId = DartsDatabase.DEFAULT_MENU_ID
    private val fastest = List(6) { listOf(3, 3, 3, 1) }.flatten() + List(5) { 2 }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DartsDatabase::class.java)
            .addCallback(DartsDatabase.seedCallback)
            .build()
        repo = PracticeRepository(db) { clock }
        backup = BackupRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun snapshot(): Triple<List<Any>, List<SessionEntity>, List<DartThrowEntity>> =
        Triple(db.menuDao().all(), db.sessionDao().all(), db.dartThrowDao().all().map { it.copy(id = 0) })

    @Test
    fun 書き出したものを読み込むと同じデータに置き換わる() = runBlocking {
        // 元のデータ: 完了した練習2回(メモ付き)と、途中の練習1回
        val a = repo.startNew(menuId)
        (listOf(0, 0) + fastest).forEach { clock += 10_000; repo.addThrow(a, Cricket10Rule, it) }
        repo.saveMemo(a, "ブルが入らない")
        clock += 600_000
        repo.insertCompleted(menuId, Cricket10Rule, clock, clock + 400_000, fastest)
        clock += 900_000
        val c = repo.startNew(menuId) // 新しく始めると途中の練習は消えるので、最後に作る
        listOf(3, 3).forEach { repo.addThrow(c, Cricket10Rule, it) }

        val before = snapshot()
        val json = BackupCodec.encode(backup.exportData(), clock)

        // 別の内容に変えてから読み込む
        repo.deleteAll()
        repo.insertCompleted(menuId, Cricket10Rule, 0, 1_000, fastest)

        backup.replaceAll(BackupCodec.decode(json))
        assertEquals(before, snapshot())
        assertEquals("ブルが入らない", db.sessionDao().get(a)!!.memo)
        assertTrue(db.sessionDao().get(a)!!.finishedAt != null)
    }

    @Test
    fun 途中で失敗したら元のデータが残る() = runBlocking {
        repo.insertCompleted(menuId, Cricket10Rule, 0, 1_000, fastest)
        val before = snapshot()

        // 同じ練習 id が2つある(主キーが重なる)ので、2つ目を入れるところで失敗する
        val good = BackupCodec.decode(BackupCodec.encode(backup.exportData(), 2_000))
        val broken = BackupData(good.menus, good.sessions + BackupSession(good.sessions[0].session, emptyList()))
        val failed = runCatching { backup.replaceAll(broken) }.isFailure

        assertTrue(failed)
        assertEquals(before, snapshot())
    }
}
