package com.example.reversealarm.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.reversealarm.receivers.LockAlarmReceiver
import java.util.Calendar

object AlarmScheduler {

    fun scheduleAlarm(context: Context, alarm: com.example.reversealarm.data.Alarm) {
        val startHour = alarm.startHour
        val startMinute = alarm.startMinute
        val endHour = alarm.endHour
        val endMinute = alarm.endMinute
        val repeatDays = alarm.getRepeatSet()
        val alarmId = alarm.id
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Should prompt user
            }
        }

        val now = Calendar.getInstance()
        
        // 1. Determine if we are "In Window" right now
        // BUT logic changes with repeats:
        // If "One-time" (repeatDays empty):
        //    - If in window -> Lock now, Schedule Stop.
        //    - If passed -> Schedule Next.
        // If "Repeats":
        //    - Check if TODAY is a valid day.
        //    - If today is valid AND in window -> Lock now, Schedule Stop.
        //    - Else find NEXT valid Start.

        // Helper to check if today is active
        // Calendar.SUNDAY = 1, MONDAY = 2, ...
        val todayDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        val isTodayActive = repeatDays.isEmpty() || repeatDays.contains(todayDayOfWeek)

        if (isTodayActive && isCurrentTimeInWindow(now, startHour, startMinute, endHour, endMinute)) {
            // Case A: Lock Immediately
            val intent = Intent(context, LockAlarmReceiver::class.java).apply { 
                putExtra("TYPE", "START")
                putExtra("ALARM_ID", alarmId)
            }
            context.sendBroadcast(intent)
            
            // Schedule Stop
            
            val stopCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, endHour)
                set(Calendar.MINUTE, endMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (stopCalendar.before(now)) {
                stopCalendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            scheduleAlarmInternally(context, stopCalendar.timeInMillis, "STOP", alarmId)

            // AND Schedule *Next* Start if repeating
            if (repeatDays.isNotEmpty()) {
                val nextStartLoop = getNextValidDay(now, startHour, startMinute, repeatDays, skipToday = true)
                scheduleAlarmInternally(context, nextStartLoop.timeInMillis, "START", alarmId)
            }
            
        } else {
            // Case B: Not currently in window. Schedule Next Start.
            val nextStart = if (repeatDays.isEmpty()) {
                // One time
                getNextTimeInstance(now, startHour, startMinute)
            } else {
                // Find next valid day
                getNextValidDay(now, startHour, startMinute, repeatDays, skipToday = false)
            }
            
            scheduleAlarmInternally(context, nextStart.timeInMillis, "START", alarmId)
            
            // For Stop: Ideally we schedule Stop when Start fires.
            // But if we want to pre-schedule:
            val nextStop = Calendar.getInstance().apply { timeInMillis = nextStart.timeInMillis }
            nextStop.set(Calendar.HOUR_OF_DAY, endHour)
            nextStop.set(Calendar.MINUTE, endMinute)
            
            if (nextStop.before(nextStart) || nextStop.timeInMillis == nextStart.timeInMillis) {
                // If End < Start (overnight), it's next day
                nextStop.add(Calendar.DAY_OF_YEAR, 1)
            } else {
                 if (nextStop.before(nextStart)) nextStop.add(Calendar.DAY_OF_YEAR, 1)
            }

            scheduleAlarmInternally(context, nextStop.timeInMillis, "STOP", alarmId)
        }
    }

    fun cancelAlarm(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Cancel START
        val startIntent = Intent(context, LockAlarmReceiver::class.java).apply {
            putExtra("TYPE", "START")
            putExtra("ALARM_ID", alarmId)
        }
        val startPending = PendingIntent.getBroadcast(
            context,
            getUniqueRequestCode("START", alarmId),
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(startPending)

        // Cancel STOP
        val stopIntent = Intent(context, LockAlarmReceiver::class.java).apply {
            putExtra("TYPE", "STOP")
            putExtra("ALARM_ID", alarmId)
        }
        val stopPending = PendingIntent.getBroadcast(
            context,
            getUniqueRequestCode("STOP", alarmId),
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(stopPending)
    }

    fun scheduleSnooze(context: Context, alarmId: Int, minutes: Int) {
        val now = Calendar.getInstance()
        now.add(Calendar.MINUTE, minutes)
        scheduleAlarmInternally(context, now.timeInMillis, "SNOOZE", alarmId)
    }

    private fun getNextTimeInstance(now: Calendar, hour: Int, minute: Int): Calendar {
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target
    }

    private fun getNextValidDay(now: Calendar, hour: Int, minute: Int, repeatDays: Set<Int>, skipToday: Boolean): Calendar {
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // If target time already passed today, we MUST skip today's check
        var startOffset = 0
        if (skipToday || target.before(now)) {
            startOffset = 1
        }
        
        // Scan up to 7 days + offset
        for (i in startOffset..7) {
            val check = Calendar.getInstance().apply { 
                timeInMillis = target.timeInMillis
                add(Calendar.DAY_OF_YEAR, i) 
            }
            val day = check.get(Calendar.DAY_OF_WEEK)
            if (repeatDays.contains(day)) {
                return check
            }
        }
        // Fallback (shouldn't happen if set not empty): Tomorrow
        target.add(Calendar.DAY_OF_YEAR, 1)
        return target
    }

    fun isCurrentTimeInWindow(now: Calendar, startH: Int, startM: Int, endH: Int, endM: Int): Boolean {
        // Simple minute-of-day math
        val currentMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMins = startH * 60 + startM
        val endMins = endH * 60 + endM

        // If start and end are exactly the same, it's either an invalid alarm or a 24h lock.
        // We'll treat it as not in window to prevent infinite loops for edge cases.
        if (startMins == endMins) return false

        if (startMins < endMins) {
            // Normal day (e.g. 09:00 to 17:00)
            return currentMins >= startMins && currentMins < endMins
        } else {
            // Overnight (e.g. 23:00 to 06:00)
            return currentMins >= startMins || currentMins < endMins
        }
    }

    private fun scheduleAlarmInternally(context: Context, timeInMillis: Long, type: String, alarmId: Int) {
        val intent = Intent(context, LockAlarmReceiver::class.java).apply {
            putExtra("TYPE", type)
            putExtra("ALARM_ID", alarmId)
        }
        
        val requestCode = getUniqueRequestCode(type, alarmId)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val clockInfo = AlarmManager.AlarmClockInfo(timeInMillis, pendingIntent)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }
    }

    private fun getUniqueRequestCode(type: String, alarmId: Int): Int {
        // More robust hash to ensure START and STOP for same ID don't collide
        val typeId = when(type) {
            "START" -> 1000000
            "STOP" -> 2000000
            "SNOOZE" -> 3000000
            else -> 4000000
        }
        return typeId + alarmId
    }
}
