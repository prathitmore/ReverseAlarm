package com.example.reversealarm.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.reversealarm.services.AdminReceiver
import com.example.reversealarm.services.RestrictionAccessibilityService

@Composable
fun PermissionScreen(
    hasOverlay: Boolean,
    hasAccessibility: Boolean,
    hasDeviceAdmin: Boolean,
    hasBatteryOptimization: Boolean,
    hasNotificationPermission: Boolean,
    hasAlarmPermission: Boolean,
    onContinue: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "To strictly enforce sleep, Reverse Alarm needs deep system access.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        PermissionItem(
            title = "1. Display Over Other Apps",
            description = "Required to show the full-screen 'Go to Sleep' overlay.",
            isGranted = hasOverlay,
            onClick = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionItem(
            title = "2. Accessibility Service",
            description = "Required to prevent you from closing the lock screen.",
            isGranted = hasAccessibility,
            onClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionItem(
            title = "3. Device Admin",
            description = "Required to prevent uninstalling the app while locked.",
            isGranted = hasDeviceAdmin,
            onClick = {
                val component = ComponentName(context, AdminReceiver::class.java)
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Prevents uninstalling during sleep hours."
                )
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionItem(
            title = "4. Ignore Battery Optimizations",
            description = "Required to allow the app to trigger locks while the phone is asleep.",
            isGranted = hasBatteryOptimization,
            onClick = {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e2: Exception) {
                            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            PermissionItem(
                title = "5. Notification Permission",
                description = "Required to show the lock status and wake you up with alarms.",
                isGranted = hasNotificationPermission,
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            PermissionItem(
                title = "6. Exact Alarm Permission",
                description = "Required to trigger the lock and ring at the precise minute scheduled.",
                isGranted = hasAlarmPermission,
                onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            enabled = hasOverlay && hasAccessibility && hasDeviceAdmin && hasBatteryOptimization &&
                    (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU || hasNotificationPermission) &&
                    (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S || hasAlarmPermission),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I'm Ready to Sleep")
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isGranted) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isGranted) {
                    Text("GRANTED", color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
            if (!isGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Grant")
                }
            }
        }
    }
}
