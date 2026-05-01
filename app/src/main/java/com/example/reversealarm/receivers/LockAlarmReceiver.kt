package com.example.reversealarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.reversealarm.services.LockOverlayService

class LockAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("TYPE") ?: return
        
        // Acquire a quick wakelock for 10 seconds to ensure service starts
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "ReverseAlarm:AlarmReceiver")
        wakeLock.acquire(10000L)

        val serviceIntent = Intent(context, LockOverlayService::class.java)
        
        if (type == "START") {
            serviceIntent.action = LockOverlayService.ACTION_START_LOCK
            startServiceCompat(context, serviceIntent)
        } else if (type == "STOP") {
            serviceIntent.action = LockOverlayService.ACTION_STOP_LOCK
            serviceIntent.putExtra("ALARM_ID", intent.getIntExtra("ALARM_ID", -1))
            startServiceCompat(context, serviceIntent)
        } else if (type == "SNOOZE") {
            val ringIntent = Intent(context, com.example.reversealarm.ui.RingScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("ALARM_ID", intent.getIntExtra("ALARM_ID", -1))
            }
            context.startActivity(ringIntent)
        }
    }

    private fun startServiceCompat(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
