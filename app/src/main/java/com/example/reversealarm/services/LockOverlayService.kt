package com.example.reversealarm.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.reversealarm.R
import com.example.reversealarm.data.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LockOverlayService : Service() {

    @Inject
    lateinit var alarmRepository: com.example.reversealarm.data.AlarmRepository

    @Inject
    lateinit var userPreferences: UserPreferencesRepository

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    
    // Ringing state
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var vibrator: android.os.Vibrator? = null
    private var isRinging = false
    
    // Hidden Exit States
    private var tapCount = 0
    private var lastTapTime: Long = 0
    private var isLongPressing = false
    private val handler = Handler(Looper.getMainLooper())
    
    private val EXIT_CODE_SENTENCE = "I ACCEPT FULL RESPONSIBILITY FOR DISABLING THIS APP"

    companion object {
        const val ACTION_START_LOCK = "ACTION_START_LOCK"
        const val ACTION_STOP_LOCK = "ACTION_STOP_LOCK"
        const val ACTION_PERMANENT_DISABLE = "ACTION_PERMANENT_DISABLE"
        const val ACTION_HIDE_OVERLAY = "ACTION_HIDE_OVERLAY"
        const val ACTION_SHOW_OVERLAY = "ACTION_SHOW_OVERLAY"
        const val ACTION_STOP_RINGING = "ACTION_STOP_RINGING"
        const val EXTRA_DURATION_MILLIS = "EXTRA_DURATION_MILLIS"
        private const val CHANNEL_ID = "LockConfigChannel"
        private const val RING_CHANNEL_ID = "AlarmRingChannel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LOCK -> {
                startLockdownSequence()
                
                // Check for auto-duration (e.g. for testing or scheduled end)
                val duration = intent.getLongExtra(EXTRA_DURATION_MILLIS, -1L)
                if (duration > 0) {
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed({
                        stopLock()
                    }, duration)
                }
            }
            ACTION_STOP_LOCK -> {
                handler.removeCallbacksAndMessages(null) // Cancel any pending auto-stop
                
                // Check if any OTHER alarm is active and valid for TODAY+NOW  
                CoroutineScope(Dispatchers.IO).launch {
                    val activeAlarms = alarmRepository.getActiveAlarmsSync()
                    val now = java.util.Calendar.getInstance()
                    val dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK)
                    
                    var shouldStayLocked = false
                    for (alarm in activeAlarms) {
                        val repeatSet = alarm.getRepeatSet()
                        val isToday = repeatSet.isEmpty() || repeatSet.contains(dayOfWeek)
                        
                        if (isToday) {
                            if (com.example.reversealarm.util.AlarmScheduler.isCurrentTimeInWindow(
                                    now,
                                    alarm.startHour,
                                    alarm.startMinute,
                                    alarm.endHour,
                                    alarm.endMinute
                                )
                            ) {
                                shouldStayLocked = true
                                break
                            }
                        }
                    }

                    if (!shouldStayLocked) {
                        // Check if we just finished an alarm that wants to RING
                        val finishingAlarmId = intent.getIntExtra("ALARM_ID", -1)
                        var shouldRing = false
                        if (finishingAlarmId != -1) {
                             val alarm = alarmRepository.getAlarm(finishingAlarmId)
                             if (alarm != null && alarm.showRingAtEnd && alarm.isArmed) {
                                 shouldRing = true
                             }
                        }

                        if (shouldRing) {
                             CoroutineScope(Dispatchers.Main).launch {
                                // Instead of just starting activity, create a Ringing Notification with FullScreenIntent
                                // This is the standard way for alarms.
                                startRingingNotification(finishingAlarmId)
                                
                                // We keep overlay active until dismissed? 
                                // Actually, if it's the end of the lock, we remove overlay
                                removeOverlay()
                            }
                        } else {
                            // Safe to unlock silently
                            CoroutineScope(Dispatchers.Main).launch {
                                stopLock()
                            }
                        }
                    } else {
                        // Refresh/Ensure overlay is visible just in case
                         CoroutineScope(Dispatchers.Main).launch {
                            if (RestrictionAccessibilityService.isLockActive) {
                                // Already locked, do nothing (or bring to front?)
                            } else {
                                // If for some reason state was confused, re-lock
                                startLockdownSequence()
                            }
                        }
                    }
                }
            }
            ACTION_PERMANENT_DISABLE -> {
                handler.removeCallbacksAndMessages(null)
                cleanupPermanently()
            }
            ACTION_HIDE_OVERLAY -> {
                overlayView?.visibility = View.GONE
            }
            ACTION_SHOW_OVERLAY -> {
                overlayView?.visibility = View.VISIBLE
            }
            ACTION_STOP_RINGING -> {
                stopRinging()
                // If we aren't locked anymore, stop self
                if (!RestrictionAccessibilityService.isLockActive) {
                    stopLock()
                }
            }
        }
        return START_STICKY
    }

    private fun startLockdownSequence() {
        startForeground(1, createNotification())
        showOverlay()
        
        // Minimize other apps immediately
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
        
        RestrictionAccessibilityService.isLockActive = true
        CoroutineScope(Dispatchers.IO).launch {
            userPreferences.setLockActive(true)
            
            // Wait slightly for Home animation to start putting apps in background
            delay(500)
            killAllOtherApps()
        }
    }

    private fun killAllOtherApps() {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager ?: return
            val pm = packageManager
            val packages = pm.getInstalledPackages(0)
            
            for (pkg in packages) {
                if (pkg.packageName != packageName && pkg.packageName != "com.android.systemui") {
                    try {
                        am.killBackgroundProcesses(pkg.packageName)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            // Show toast on main thread
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "Closing background apps...", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopLock() {
        stopRinging()
        removeOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        RestrictionAccessibilityService.isLockActive = false
        CoroutineScope(Dispatchers.IO).launch {
            userPreferences.setLockActive(false)
        }
    }

    private fun cleanupPermanently() {
        // Clear flags and stop
        RestrictionAccessibilityService.isLockActive = false
        CoroutineScope(Dispatchers.IO).launch {
            userPreferences.setLockdownDisabled(true)
            userPreferences.setHiddenExitUsed(true)
            userPreferences.setLockActive(false)
        }
        stopLock()
        // Here we would also cancel alarms via AlarmManager if we had the logic connected
    }

    private fun showOverlay() {
        if (overlayView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.screenOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.view_lock_overlay, null)
        
        setupHiddenExitLogic(overlayView!!)

        overlayView?.isFocusable = true
        overlayView?.isFocusableInTouchMode = true
        overlayView?.setOnKeyListener { _, keyCode, event ->
            // Trap Back button
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                return@setOnKeyListener true 
            }
            return@setOnKeyListener false
        }

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupHiddenExitLogic(view: View) {
        val tvGoToSleep = view.findViewById<TextView>(R.id.tvGoToSleep)
        
        tvGoToSleep.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastTapTime > 1000) {
                tapCount = 1
            } else {
                tapCount++
            }
            lastTapTime = now
        }

        tvGoToSleep.setOnTouchListener { v, event ->
            if (tapCount >= 7) {
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        isLongPressing = true
                        handler.postDelayed(cleanupRunnable, 10000) // 10s Hold
                        true
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        isLongPressing = false
                        handler.removeCallbacks(cleanupRunnable)
                        true
                    }
                    else -> false
                }
            } else {
                false // Let click listener handle taps
            }
        }
    }

    private val cleanupRunnable = Runnable {
        if (isLongPressing) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "Emergency Exit Activated", Toast.LENGTH_LONG).show()
                cleanupPermanently()
            }
        }
    }

    private fun removeOverlay() {
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Lock Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            
            val ringChannel = NotificationChannel(
                RING_CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(ringChannel)
        }
    }

    private fun startRingingNotification(alarmId: Int) {
        isRinging = true
        
        val ringIntent = Intent(this, com.example.reversealarm.ui.RingScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("ALARM_ID", alarmId)
        }
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 1001, ringIntent, 
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, RING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app icon
            .setContentTitle("WAKE UP!")
            .setContentText("Your scheduled lock has ended.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
            
        startForeground(2, notification) // Higher ID for ring
        
        // Start Sound/Vibration
        CoroutineScope(Dispatchers.IO).launch {
             val alarm = alarmRepository.getAlarm(alarmId)
             CoroutineScope(Dispatchers.Main).launch {
                 playRingtone(alarm?.ringtoneUri)
                 if (alarm?.isVibrate != false) startVibration()
             }
        }
    }

    private fun playRingtone(uriString: String?) {
        try {
            val uri = if (uriString != null) android.net.Uri.parse(uriString) else android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            mediaPlayer?.stop()
            mediaPlayer?.release()
            
            mediaPlayer = android.media.MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 500, 1000), 0))
        } else {
            vibrator?.vibrate(longArrayOf(0, 500, 1000), 0)
        }
    }

    private fun stopRinging() {
        isRinging = false
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Reverse Alarm Active")
            .setContentText("Phone is locked. Go to sleep.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }
}
