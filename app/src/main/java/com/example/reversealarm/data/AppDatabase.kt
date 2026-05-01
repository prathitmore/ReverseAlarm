package com.example.reversealarm.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Alarm::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
}
