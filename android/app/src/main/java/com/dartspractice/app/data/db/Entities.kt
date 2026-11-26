package com.dartspractice.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 練習メニュー。ruleType で PracticeRule を選ぶ。 */
@Entity(tableName = "menu")
data class MenuEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ruleType: String,
)

/**
 * 1回の練習。日時はミリ秒(エポック)。
 * finishedAt はブル完了日時で、日ごとの集計はこの日付で行う。未完了なら null。
 */
@Entity(
    tableName = "session",
    foreignKeys = [ForeignKey(entity = MenuEntity::class, parentColumns = ["id"], childColumns = ["menuId"])],
    indices = [Index("menuId"), Index("finishedAt")],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val menuId: Long,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val finished: Boolean = false,
    val memo: String = "",
)

/** 1投。練習を消すと一緒に消える。 */
@Entity(
    tableName = "dart_throw",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sessionId", "seq"], unique = true)],
)
data class DartThrowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val seq: Int,
    val target: String,
    val rawMarks: Int,
    val marks: Int,
)
