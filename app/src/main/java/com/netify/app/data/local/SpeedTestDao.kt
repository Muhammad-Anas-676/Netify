package com.netify.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedTestDao {
    @Insert
    suspend fun insert(entity: SpeedTestEntity): Long

    @Query("SELECT * FROM speed_test_history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SpeedTestEntity>>

    @Query("SELECT * FROM speed_test_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): SpeedTestEntity?

    @Query("SELECT * FROM speed_test_history WHERE timestamp >= :sinceEpochMillis ORDER BY timestamp DESC")
    fun observeSince(sinceEpochMillis: Long): Flow<List<SpeedTestEntity>>

    @Delete
    suspend fun delete(entity: SpeedTestEntity)

    @Query("DELETE FROM speed_test_history")
    suspend fun clearAll()
}
