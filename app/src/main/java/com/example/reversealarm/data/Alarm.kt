package com.example.reversealarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val isArmed: Boolean,
    val repeatDays: String = "", // CSV e.g "2,3,4"
    val ringtoneUri: String? = null,
    val isVibrate: Boolean = true,
    val snoozeConfig: String = "5 minutes, 3 times",
    val showRingAtEnd: Boolean = true
) {
    fun getRepeatSet(): Set<Int> {
        if (repeatDays.isEmpty()) return emptySet()
        return repeatDays.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }
}
