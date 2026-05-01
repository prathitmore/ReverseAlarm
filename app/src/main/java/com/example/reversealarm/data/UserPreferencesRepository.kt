package com.example.reversealarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val PREF_LOCK_ACTIVE = booleanPreferencesKey("lock_active")
    private val PREF_LOCKDOWN_DISABLED_PERMANENTLY = booleanPreferencesKey("lockdown_disabled_perm")
    private val PREF_HIDDEN_EXIT_USED = booleanPreferencesKey("hidden_exit_used")
    private val PREF_HAS_SEEN_TUTORIAL = booleanPreferencesKey("has_seen_tutorial")
    
    // Schedule
    private val PREF_START_HOUR = intPreferencesKey("start_hour")
    private val PREF_START_MINUTE = intPreferencesKey("start_minute")
    private val PREF_END_HOUR = intPreferencesKey("end_hour")
    private val PREF_END_MINUTE = intPreferencesKey("end_minute")
    private val PREF_IS_ARMED = booleanPreferencesKey("is_armed")
    
    // Cooldown
    private val PREF_LOCK_START_TIMESTAMP = longPreferencesKey("lock_start_timestamp")

    val isLockdownDisabled: Flow<Boolean> = context.dataStore.data.map { it[PREF_LOCKDOWN_DISABLED_PERMANENTLY] ?: false }
    val isArmed: Flow<Boolean> = context.dataStore.data.map { it[PREF_IS_ARMED] ?: false }
    val hasSeenTutorial: Flow<Boolean> = context.dataStore.data.map { it[PREF_HAS_SEEN_TUTORIAL] ?: false }

    // Advanced
    private val PREF_REPEAT_DAYS = stringPreferencesKey("repeat_days") // CSV "1,2,3"
    private val PREF_RINGTONE_URI = stringPreferencesKey("ringtone_uri")
    private val PREF_IS_VIBRATE = booleanPreferencesKey("is_vibrate")
    private val PREF_SNOOZE_CONFIG = stringPreferencesKey("snooze_config")

    data class Schedule(
        val startHour: Int, 
        val startMinute: Int, 
        val endHour: Int, 
        val endMinute: Int, 
        val isArmed: Boolean,
        val repeatDays: Set<Int> = emptySet(),
        val ringtoneUri: String? = null,
        val isVibrate: Boolean = true,
        val snoozeConfig: String = "5 minutes, 3 times"
    )

    val scheduleFlow: Flow<Schedule?> = context.dataStore.data.map { preferences ->
        val startH = preferences[PREF_START_HOUR]
        val startM = preferences[PREF_START_MINUTE]
        val endH = preferences[PREF_END_HOUR]
        val endM = preferences[PREF_END_MINUTE]
        val armed = preferences[PREF_IS_ARMED] ?: false
        
        val repeatStr = preferences[PREF_REPEAT_DAYS] ?: ""
        val repeatDays = if (repeatStr.isNotEmpty()) repeatStr.split(",").mapNotNull { it.toIntOrNull() }.toSet() else emptySet()
        val ringtone = preferences[PREF_RINGTONE_URI]
        val vibrate = preferences[PREF_IS_VIBRATE] ?: true
        val snooze = preferences[PREF_SNOOZE_CONFIG] ?: "5 minutes, 3 times"

        if (startH != null && startM != null && endH != null && endM != null) {
            Schedule(startH, startM, endH, endM, armed, repeatDays, ringtone, vibrate, snooze)
        } else {
            null
        }
    }

    suspend fun setLockdownDisabled(disabled: Boolean) {
        context.dataStore.edit { it[PREF_LOCKDOWN_DISABLED_PERMANENTLY] = disabled }
    }

    suspend fun setHiddenExitUsed(used: Boolean) {
        context.dataStore.edit { it[PREF_HIDDEN_EXIT_USED] = used }
    }
    
    suspend fun setHasSeenTutorial(seen: Boolean) {
        context.dataStore.edit { it[PREF_HAS_SEEN_TUTORIAL] = seen }
    }
    
    suspend fun setSchedule(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int, armed: Boolean) {
        context.dataStore.edit { 
            it[PREF_START_HOUR] = startHour
            it[PREF_START_MINUTE] = startMinute
            it[PREF_END_HOUR] = endHour
            it[PREF_END_MINUTE] = endMinute
            it[PREF_IS_ARMED] = armed
        }
    }
    
    suspend fun setSchedule(
        startHour: Int, 
        startMinute: Int, 
        endHour: Int, 
        endMinute: Int, 
        armed: Boolean,
        repeatDays: Set<Int>? = null,
        ringtoneUri: String? = null,
        isVibrate: Boolean? = null,
        snoozeConfig: String? = null
    ) {
        context.dataStore.edit { 
            it[PREF_START_HOUR] = startHour
            it[PREF_START_MINUTE] = startMinute
            it[PREF_END_HOUR] = endHour
            it[PREF_END_MINUTE] = endMinute
            it[PREF_IS_ARMED] = armed
            
            if (repeatDays != null) {
                it[PREF_REPEAT_DAYS] = repeatDays.joinToString(",")
            }
            if (ringtoneUri != null) {
                it[PREF_RINGTONE_URI] = ringtoneUri
            }
            if (isVibrate != null) {
                it[PREF_IS_VIBRATE] = isVibrate
            }
            if (snoozeConfig != null) {
                it[PREF_SNOOZE_CONFIG] = snoozeConfig
            }
        }
    }
    
    suspend fun setLockActive(active: Boolean) {
        context.dataStore.edit { 
            it[PREF_LOCK_ACTIVE] = active
            if (active) {
                it[PREF_LOCK_START_TIMESTAMP] = System.currentTimeMillis()
            }
        }
    }
}
