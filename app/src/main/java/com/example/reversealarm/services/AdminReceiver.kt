package com.example.reversealarm.services

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Reverse Alarm Protection Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Reverse Alarm Protection Disabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence? {
        // In a real scenario, we might want to check if sleep mode is active and warn the user.
        return "Are you sure? This will disable the sleep enforcement protection."
    }
}
