package com.example.reversealarm.services

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class RestrictionAccessibilityService : AccessibilityService() {

    // Ideally, we would inject a repository or manager here to check if lock is active
    // @Inject lateinit var lockManager: LockManager 
    
    // For MVP, we'll use a static flag or shared preference. 
    // This is a known anti-pattern but acceptable for prototype speed if scope limited.
    companion object {
        var isLockActive = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Configuration is set via XML
    }

    // State to avoid spamming service calls
    private var isOverlayHidden = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isLockActive) return
        
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
             val currentPackage = event.packageName?.toString() ?: return
             
             // Check if Dialer (Incoming/Active Call)
             if (isDialerApp(currentPackage)) {
                 if (!isOverlayHidden) {
                     val intent = android.content.Intent(this, LockOverlayService::class.java)
                     intent.action = LockOverlayService.ACTION_HIDE_OVERLAY
                     startService(intent)
                     isOverlayHidden = true
                 }
                 return // Allow interaction
             }

             // If we are here, it's NOT Dialer. Ensure overlay is visible if it was hidden.
             if (isOverlayHidden) {
                 val intent = android.content.Intent(this, LockOverlayService::class.java)
                 intent.action = LockOverlayService.ACTION_SHOW_OVERLAY
                 startService(intent)
                 isOverlayHidden = false
             }

             // Allow ONLY our app
             if (currentPackage != "com.example.reversealarm") {
                 
                 // If it's SystemUI (Shade, Quick Settings)
                 if (currentPackage == "com.android.systemui") {
                     performGlobalAction(GLOBAL_ACTION_BACK)
                     if (android.os.Build.VERSION.SDK_INT >= 31) {
                         performGlobalAction(15)
                     }
                     performGlobalAction(GLOBAL_ACTION_HOME)
                 } else {
                     // For other apps, just go Home
                     performGlobalAction(GLOBAL_ACTION_HOME)
                 }
             }
        }
    }

    private fun isDialerApp(packageName: String): Boolean {
        return try {
            val telecomManager = getSystemService(android.content.Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
            val defaultDialer = telecomManager?.defaultDialerPackage
            packageName == defaultDialer || packageName.contains("dialer") || packageName.contains("incallui") || packageName.contains("phone")
        } catch (e: Exception) {
            false
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!isLockActive) return super.onKeyEvent(event)

        // Block Back, Home, Recent Apps buttons if possible.
        // Also block Notification/Settings keys if present.
        return when (event.keyCode) {
            KeyEvent.KEYCODE_BACK, 
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_NOTIFICATION, // Block dedicated notification key
            KeyEvent.KEYCODE_SETTINGS -> true // Consume event
            else -> super.onKeyEvent(event)
        }
    }
}
