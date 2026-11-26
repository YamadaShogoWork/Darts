package com.dartspractice.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MenuDao {
    @Query("SELECT * FROM menu ORDER BY id LIMIT 1")
    suspend fun first(): MenuEntity?

    @Query("SELECT * FROM menu WHERE id = :id")
    suspend fun get(id: Long): MenuEntity?

    @Query("SELECT * FROM menu WHERE ruleType = :ruleType ORDER BY id LIMIT 1")
    suspend fun byRuleType(ruleType: String): MenuEntity?

    @Insert
    suspend fun insert(menu: MenuEntity): Long

    @Query("SELECT * FROM menu ORDER BY id")
    suspend fun all(): List<MenuEntity>

    /** すべてのメニューを消す(先に練習を消しておくこと)。 */
    @Query("DELETE FROM menu")
    suspend fun deleteAll()
}

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM session WHERE id = :id")
    suspend fun get(id: Long): SessionEntity?

    @Query("SELECT * FROM session ORDER BY id")
    suspend fun all(): List<SessionEntity>

    /** そのメニューの未完了の練習(新しい順)。 */
    @Query("SELECT * FROM session WHERE menuId = :menuId AND finished = 0 ORDER BY startedAt DESC")
    suspend fun unfinished(menuId: Long): List<SessionEntity>

    @Query("UPDATE session SET finished = :finished, finishedAt = :finishedAt WHERE id = :id")
    suspend fun setFinished(id: Long, finished: Boolean, finishedAt: Long?)

    @Query("UPDATE session SET memo = :memo WHERE id = :id")
    suspend fun setMemo(id: Long, memo: String)

    @Query("DELETE FROM session WHERE id = :id")
    suspend fun delete(id: Long)

    /** すべての練習を消す。投も一緒に消える。 */
    @Query("DELETE FROM session")
    suspend fun deleteAll()
}

@Dao
interface DartThrowDao {
    @Insert
    suspend fun insert(dartThrow: DartThrowEntity): Long

    @Query("SELECT * FROM dart_throw WHERE sessionId = :sessionId ORDER BY seq")
    suspend fun list(sessionId: Long): List<DartThrowEntity>

    @Query("SELECT * FROM dart_throw ORDER BY sessionId, seq")
    suspend fun all(): List<DartThrowEntity>

    @Query("SELECT COUNT(*) FROM dart_throw WHERE sessionId = :sessionId")
    suspend fun count(sessionId: Long): Int

    @Query("DELETE FROM dart_throw WHERE sessionId = :sessionId AND seq = :seq")
    suspend fun delete(sessionId: Long, seq: Int)
}
