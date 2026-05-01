package com.example.reversealarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.reversealarm.data.UserPreferencesRepository
import com.example.reversealarm.util.AlarmScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun alarmRepository(): com.example.reversealarm.data.AlarmRepository
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            
            // Manual Injection
            val appContext = context.applicationContext ?: throw IllegalStateException("No App Context")
            val entryPoint = EntryPointAccessors.fromApplication(appContext, BootReceiverEntryPoint::class.java)
            val alarmRepository = entryPoint.alarmRepository()

            // 1. Reschedule Alarms
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarms = alarmRepository.getActiveAlarmsSync()
                    for (alarm in alarms) {
                        AlarmScheduler.scheduleAlarm(context, alarm)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
