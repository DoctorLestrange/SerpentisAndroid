package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM run_records ORDER BY score DESC, timestamp DESC")
    fun getAllRunsSortedByScore(): Flow<List<RunRecordEntity>>

    @Query("SELECT * FROM run_records ORDER BY timestamp DESC")
    fun getAllRunsRecent(): Flow<List<RunRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRunRecord(record: RunRecordEntity)

    @Query("DELETE FROM run_records")
    suspend fun clearAllRuns()

    @Query("SELECT * FROM saved_game WHERE slotId = 1 LIMIT 1")
    fun getSavedGameFlow(): Flow<SavedGameEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedGame(savedGame: SavedGameEntity)

    @Query("DELETE FROM saved_game WHERE slotId = 1")
    suspend fun clearSavedGame()

    @Query("SELECT * FROM achievements")
    fun getAllAchievementsFlow(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievementsSnapshot(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievements(items: List<AchievementEntity>)
}
