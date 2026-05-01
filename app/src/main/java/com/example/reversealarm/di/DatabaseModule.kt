package com.example.reversealarm.di

import android.content.Context
import androidx.room.Room
import com.example.reversealarm.data.AlarmDao
import com.example.reversealarm.data.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "alarm_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideAlarmDao(database: AppDatabase): AlarmDao {
        return database.alarmDao()
    }
}
