package com.example.reversealarm.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms")
    fun getAllAlarms(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarm(id: Int): Alarm?

    @Query("SELECT * FROM alarms WHERE isArmed = 1")
    fun getActiveAlarms(): Flow<List<Alarm>>
    
    @Query("SELECT * FROM alarms WHERE isArmed = 1")
    suspend fun getActiveAlarmsSync(): List<Alarm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm): Long

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun deleteAlarm(alarm: Alarm)

    @Query("DELETE FROM alarms")
    suspend fun deleteAllAlarms()
}
