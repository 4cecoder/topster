package com.topster.tv.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY lastWatched DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 50): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE completed = 0 ORDER BY lastWatched DESC")
    fun getIncompleteHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE mediaId = :mediaId AND episodeId = :episodeId LIMIT 1")
    suspend fun getHistoryEntry(mediaId: String, episodeId: String?): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long

    @Update
    suspend fun updateHistory(history: HistoryEntity)

    @Query("DELETE FROM watch_history WHERE mediaId = :mediaId AND episodeId = :episodeId")
    suspend fun deleteHistoryEntry(mediaId: String, episodeId: String?)

    @Query("SELECT * FROM watch_history ORDER BY lastWatched DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE title = :showTitle ORDER BY lastWatched DESC")
    fun getHistoryByShow(showTitle: String): Flow<List<HistoryEntity>>

    @Query("DELETE FROM watch_history")
    suspend fun clearAllHistory()

    @Query("SELECT COUNT(*) FROM watch_history")
    suspend fun getHistoryCount(): Int
}
