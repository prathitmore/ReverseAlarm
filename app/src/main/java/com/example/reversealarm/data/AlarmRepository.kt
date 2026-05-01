package com.example.reversealarm.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class AlarmRepository @Inject constructor(
    private val alarmDao: AlarmDao
) {
    val allAlarms: Flow<List<Alarm>> = alarmDao.getAllAlarms()
    val activeAlarms: Flow<List<Alarm>> = alarmDao.getActiveAlarms()

    suspend fun getAlarm(id: Int): Alarm? = alarmDao.getAlarm(id)
    
    suspend fun insert(alarm: Alarm): Long = alarmDao.insertAlarm(alarm)
    
    suspend fun update(alarm: Alarm) = alarmDao.updateAlarm(alarm)
    
    suspend fun delete(alarm: Alarm) = alarmDao.deleteAlarm(alarm)

    suspend fun deleteAll() = alarmDao.deleteAllAlarms()

    suspend fun getActiveAlarmsSync(): List<Alarm> = alarmDao.getActiveAlarmsSync()
}
