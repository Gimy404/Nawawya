package com.nawawia.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "hadith_progress")
data class HadithProgressEntity(
    @PrimaryKey val hadithId: String,
    val easeFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val dueEpochDay: Long = 0L,
    val masteryPct: Int = 0,
    val lastGrade: Int = 0,
    val isBookmarked: Boolean = false,
    val completedCount: Int = 0
)

@Entity(tableName = "user_state")
data class UserStateEntity(
    @PrimaryKey val id: Int = 1,
    val totalXp: Int = 0,
    val currentHearts: Int = 5,
    val lastHeartSpentEpochMillis: Long? = null,
    val streakDays: Int = 0,
    val lastActiveEpochDay: Long? = null,
    val freezesAvailable: Int = 0,
    val activeEpochDaysCsv: String = "",
    val clockRollbackCount: Int = 0,
    val completedQuickReview10Min: Boolean = false,
    val totalMinutesSpent: Int = 0,
    val dailyGoalMinutes: Int = 10,
    val onboardingCompleted: Boolean = false,
    val themeMode: String = "LIGHT", // LIGHT, DARK, SYSTEM
    val largeFontEnabled: Boolean = false,
    val tashkeelEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true
)

@Entity(tableName = "active_lesson_session")
data class ActiveLessonSessionEntity(
    @PrimaryKey val id: Int = 1,
    val hadithId: String,
    val seed: Long,
    val questionIdsCsv: String,
    val currentQuestionIndex: Int,
    val correctFirstTryCount: Int,
    val correctAfterRetryCount: Int,
    val retriesCount: Int,
    val isReviewMode: Boolean
)

@Dao
interface NawawiaDao {

    @Query("SELECT * FROM hadith_progress")
    fun observeAllHadithProgress(): Flow<List<HadithProgressEntity>>

    @Query("SELECT * FROM hadith_progress")
    suspend fun getAllHadithProgress(): List<HadithProgressEntity>

    @Query("SELECT * FROM hadith_progress WHERE hadithId = :hadithId LIMIT 1")
    suspend fun getHadithProgress(hadithId: String): HadithProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHadithProgress(entity: HadithProgressEntity)

    @Query("SELECT * FROM user_state WHERE id = 1 LIMIT 1")
    fun observeUserState(): Flow<UserStateEntity?>

    @Query("SELECT * FROM user_state WHERE id = 1 LIMIT 1")
    suspend fun getUserState(): UserStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserState(entity: UserStateEntity)

    @Query("SELECT * FROM active_lesson_session WHERE id = 1 LIMIT 1")
    suspend fun getActiveLessonSession(): ActiveLessonSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActiveLessonSession(session: ActiveLessonSessionEntity)

    @Query("DELETE FROM active_lesson_session")
    suspend fun clearActiveLessonSession()

    @Query("DELETE FROM hadith_progress")
    suspend fun clearAllHadithProgress()

    @Query("DELETE FROM user_state")
    suspend fun clearUserState()

    @Transaction
    suspend fun recordAnswerTransaction(
        session: ActiveLessonSessionEntity,
        userState: UserStateEntity
    ) {
        upsertActiveLessonSession(session)
        upsertUserState(userState)
    }

    @Transaction
    suspend fun completeLessonTransaction(
        hadithProgress: HadithProgressEntity,
        userState: UserStateEntity
    ) {
        upsertHadithProgress(hadithProgress)
        upsertUserState(userState)
        clearActiveLessonSession()
    }

    @Transaction
    suspend fun resetAllDataTransaction() {
        clearActiveLessonSession()
        clearAllHadithProgress()
        clearUserState()
        upsertUserState(UserStateEntity())
    }
}

@Database(
    entities = [
        HadithProgressEntity::class,
        UserStateEntity::class,
        ActiveLessonSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NawawiaDatabase : RoomDatabase() {
    abstract fun dao(): NawawiaDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Prepared migration path from v1 -> v2; never uses destructive migration
                db.execSQL("ALTER TABLE user_state ADD COLUMN reminderHour INTEGER NOT NULL DEFAULT 20")
            }
        }

        fun build(context: Context, inMemory: Boolean = false): NawawiaDatabase {
            val builder = if (inMemory) {
                Room.inMemoryDatabaseBuilder(context, NawawiaDatabase::class.java)
                    .allowMainThreadQueries()
            } else {
                Room.databaseBuilder(context, NawawiaDatabase::class.java, "nawawia.db")
            }
            return builder
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
