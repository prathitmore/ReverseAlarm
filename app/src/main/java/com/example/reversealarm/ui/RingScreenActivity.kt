package com.example.reversealarm.ui

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reversealarm.data.AlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RingScreenActivity : ComponentActivity() {

    @Inject
    lateinit var alarmRepository: AlarmRepository

    // Removed local media management, handled by LockOverlayService now
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        turnScreenOnAndKeyguardOff()

        val alarmId = intent.getIntExtra("ALARM_ID", -1)

        setContent {
            RingScreen(
                onDismiss = {
                    stopServiceRinging()
                    finish()
                },
                onSnooze = {
                    stopServiceRinging()
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        val snoozeMinutes = if (alarmId != -1) {
                            try {
                                val alarm = alarmRepository.getAlarm(alarmId)
                                val snoozeString = alarm?.snoozeConfig ?: "5"
                                val match = Regex("\\d+").find(snoozeString)
                                match?.value?.toInt() ?: 5
                            } catch (e: Exception) { 5 }
                        } else { 5 }
                        
                        com.example.reversealarm.util.AlarmScheduler.scheduleSnooze(applicationContext, alarmId, snoozeMinutes)
                        
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(applicationContext, "Snoozed for $snoozeMinutes minutes", android.widget.Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
            )
        }
    }

    private fun stopServiceRinging() {
        val intent = Intent(this, com.example.reversealarm.services.LockOverlayService::class.java).apply {
            action = com.example.reversealarm.services.LockOverlayService.ACTION_STOP_RINGING
        }
        startService(intent)
    }

    private fun turnScreenOnAndKeyguardOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null)
        }
    }

    private fun stopRinging() {
        // No-op, managed by service
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@Composable
fun RingScreen(onDismiss: () -> Unit, onSnooze: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = "Good Morning!",
            color = Color.White,
            fontSize = 32.sp
        )
        
        // Pulse Animation Placeholder
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color(0xFF448AFF).copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("⏰", fontSize = 64.sp)
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Snooze", fontSize = 18.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text("Stop", fontSize = 18.sp)
            }
        }
    }
}
