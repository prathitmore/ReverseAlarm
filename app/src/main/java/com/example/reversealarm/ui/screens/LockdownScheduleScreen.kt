package com.example.reversealarm.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.reversealarm.data.UserPreferencesRepository
import com.example.reversealarm.util.AlarmScheduler
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockdownScheduleScreen(
    userPreferences: UserPreferencesRepository,
    alarmRepository: com.example.reversealarm.data.AlarmRepository, // Add Repository
    alarmId: Int? = null,
    isEditMode: Boolean = false, // Kept for UI logic, but effectively checking alarmId too
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Default values
    val defaultStart = remember { java.util.Calendar.getInstance().apply { add(java.util.Calendar.MINUTE, 5) } }
    val defaultEnd = remember { java.util.Calendar.getInstance().apply { add(java.util.Calendar.MINUTE, 6) } }
    
    var startHour by remember { mutableStateOf(defaultStart.get(java.util.Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableStateOf(defaultStart.get(java.util.Calendar.MINUTE)) }
    var endHour by remember { mutableStateOf(defaultEnd.get(java.util.Calendar.HOUR_OF_DAY)) }
    var endMinute by remember { mutableStateOf(defaultEnd.get(java.util.Calendar.MINUTE)) }
    var repeatDays by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var ringtoneUri by remember { mutableStateOf<String?>(null) }
    var isVibrate by remember { mutableStateOf(true) }
    var snoozeConfig by remember { mutableStateOf("5 minutes, 3 times") }
    var showRingAtEnd by remember { mutableStateOf(true) }
    var isArmed by remember { mutableStateOf(true) }

    // Use refs to avoid recomposing the entire screen on every dialer tick
    val startRef = remember { intArrayOf(defaultStart.get(java.util.Calendar.HOUR_OF_DAY), defaultStart.get(java.util.Calendar.MINUTE)) }
    val endRef = remember { intArrayOf(defaultEnd.get(java.util.Calendar.HOUR_OF_DAY), defaultEnd.get(java.util.Calendar.MINUTE)) }

    // Load existing alarm if editing
    LaunchedEffect(alarmId) {
        if (alarmId != null) {
            val alarm = alarmRepository.getAlarm(alarmId)
            if (alarm != null) {
                startHour = alarm.startHour
                startMinute = alarm.startMinute
                endHour = alarm.endHour
                endMinute = alarm.endMinute
                repeatDays = alarm.getRepeatSet()
                ringtoneUri = alarm.ringtoneUri
                isVibrate = alarm.isVibrate
                snoozeConfig = alarm.snoozeConfig
                isArmed = alarm.isArmed
                showRingAtEnd = alarm.showRingAtEnd

                startRef[0] = alarm.startHour
                startRef[1] = alarm.startMinute
                endRef[0] = alarm.endHour
                endRef[1] = alarm.endMinute
            }
        } else {
            startRef[0] = startHour
            startRef[1] = startMinute
            endRef[0] = endHour
            endRef[1] = endMinute
        }
    }
    
    // State for Dialog Pickers
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // --- Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("Cancel", color = Color(0xFF64B5F6), fontSize = 16.sp)
            }
            Text(if (isEditMode) "Edit alarm" else "New alarm", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = {
                 scope.launch {
                    val repeatString = repeatDays.joinToString(",")
                val newAlarm = com.example.reversealarm.data.Alarm(
                        id = alarmId ?: 0, // 0 means auto-generate if insert
                        startHour = startRef[0],
                        startMinute = startRef[1],
                        endHour = endRef[0],
                        endMinute = endRef[1],
                        isArmed = isArmed,
                        repeatDays = repeatString,
                        ringtoneUri = ringtoneUri,
                        isVibrate = isVibrate,
                        snoozeConfig = snoozeConfig,
                        showRingAtEnd = showRingAtEnd
                    )
                    
                    if (alarmId != null) {
                        alarmRepository.update(newAlarm)
                        AlarmScheduler.cancelAlarm(context, alarmId) // Cancel old
                        AlarmScheduler.scheduleAlarm(context, newAlarm) // Schedule new
                    } else {
                        val newId = alarmRepository.insert(newAlarm).toInt()
                        // Schedule with new ID
                        val insertedAlarm = newAlarm.copy(id = newId)
                        AlarmScheduler.scheduleAlarm(context, insertedAlarm)
                    }
                    
                    onBack()
                }
            }) {
                Text(if (isEditMode) "Done" else "Save", color = Color(0xFF64B5F6), fontSize = 16.sp)
            }
        }
        
        // --- Main Content ---
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Spacer(modifier = Modifier.height(16.dp))

            if (isEditMode) {
                // --- EDIT MODE LAYOUT ---
                Text("Start Time", color = Color.Gray, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
                
                TimeWheelPicker(
                    initialHour = startHour, 
                    initialMinute = startMinute,
                    onTimeChanged = { h, m -> 
                        startRef[0] = h
                        startRef[1] = m
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("End Time", color = Color.Gray, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
                
                TimeWheelPicker(
                    initialHour = endHour, 
                    initialMinute = endMinute,
                    onTimeChanged = { h, m -> 
                        endRef[0] = h
                        endRef[1] = m
                    }
                )
            } else {
                // --- CREATE MODE LAYOUT ---
                // "Like before 2 separate clocks with big digits"
                
                TimeDisplayRow("Lock starts at", startHour, startMinute) { showStartTimePicker = true }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TimeDisplayRow("Lock ends at", endHour, endMinute) { showEndTimePicker = true }
            }

            // --- Quick Actions Row (Ring Once, Workdays, Custom) ---
            var selectedRepeatOption by remember { mutableStateOf("Custom") }
            
            // Sync initial state
            LaunchedEffect(repeatDays) {
                if (repeatDays.isEmpty()) selectedRepeatOption = "Once"
                else if (repeatDays.size == 5 && repeatDays.containsAll(listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY))) selectedRepeatOption = "Workdays"
                else selectedRepeatOption = "Custom"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = listOf("Once" to "Once", "Workdays" to "Workdays", "Custom" to "Custom")
                options.forEach { (label, key) ->
                    val isSelected = selectedRepeatOption == key
                    Button(
                        onClick = {
                            selectedRepeatOption = key
                            when (key) {
                                "Once" -> repeatDays = emptySet()
                                "Workdays" -> repeatDays = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
                                "Custom" -> {
                                    // If switching to Custom from Empty, maybe default to Today? Or just keep as is?
                                    // If switching from Workdays, keep Workdays as start point.
                                    // Generally, just clicking Custom shouldn't destructively wipe unless needed.
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF448AFF) else Color(0xFF333333),
                            contentColor = if (isSelected) Color.White else Color.Gray
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(label, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Repeat", color = Color.White, fontSize = 16.sp)
                        // Summary text
                        Text(getRepeatSummary(repeatDays), color = Color.Gray, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Weekday Bubbles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val days = listOf(
                            "S" to Calendar.SUNDAY, 
                            "M" to Calendar.MONDAY, 
                            "T" to Calendar.TUESDAY, 
                            "W" to Calendar.WEDNESDAY, 
                            "T" to Calendar.THURSDAY, 
                            "F" to Calendar.FRIDAY, 
                            "S" to Calendar.SATURDAY
                        )
                        
                        days.forEach { (label, calendarDay) ->
                            val isSelected = repeatDays.contains(calendarDay)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFF448AFF) else Color(0xFF333333))
                                    .clickable {
                                        // Auto-switch to Custom if modifying specific days
                                        selectedRepeatOption = "Custom"
                                        repeatDays = if (isSelected) {
                                            repeatDays - calendarDay
                                        } else {
                                            repeatDays + calendarDay
                                        }
                                    }
                            ) {
                                Text(
                                    text = label, 
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // --- Options Card (Ringtone, Vibrate, Snooze) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Ring Enable Toggle
                    OptionItemToggle(
                        label = "Ring",
                        checked = showRingAtEnd,
                        onCheckedChange = { showRingAtEnd = it }
                    )
                    Divider(color = Color(0xFF2C2C2E), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    // Ringtone
                    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                            ringtoneUri = uri?.toString()
                        }
                    }
                    
                    OptionItem(
                        label = "Ringtone", 
                        value = getRingtoneName(context, ringtoneUri),
                        onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                            val currentUri = if (ringtoneUri != null) Uri.parse(ringtoneUri) else null
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
                            launcher.launch(intent)
                        }
                    )
                    Divider(color = Color(0xFF2C2C2E), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // Vibrate
                    OptionItemToggle(
                        label = "Vibrate",
                        checked = isVibrate,
                        onCheckedChange = { isVibrate = it }
                    )
                    Divider(color = Color(0xFF2C2C2E), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // Snooze
                    OptionItem(
                        label = "Snooze",
                        value = snoozeConfig,
                        onClick = {
                            // Cycle simple options for now or show dialog
                            if (snoozeConfig == "Off") snoozeConfig = "5 minutes, 3 times"
                            else snoozeConfig = "Off"
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            

        }
    }

    // Dialogs
    // Dialogs
    // Dialogs
    if (showStartTimePicker) {
        if (isEditMode) {
             WheelTimePickerDialog(
                initialHour = startHour,
                initialMinute = startMinute,
                onConfirm = { h, m ->
                    startHour = h
                    startMinute = m
                    startRef[0] = h
                    startRef[1] = m
                    showStartTimePicker = false
                },
                onDismiss = { showStartTimePicker = false }
            )
        } else {
            DialTimePickerDialog(
                initialHour = startHour,
                initialMinute = startMinute,
                onConfirm = { h, m ->
                    startHour = h
                    startMinute = m
                    startRef[0] = h
                    startRef[1] = m
                    showStartTimePicker = false
                },
                onDismiss = { showStartTimePicker = false }
            )
        }
    }
    
    if (showEndTimePicker) {
        if (isEditMode) {
            WheelTimePickerDialog(
                initialHour = endHour,
                initialMinute = endMinute,
                onConfirm = { h, m ->
                    endHour = h
                    endMinute = m
                    endRef[0] = h
                    endRef[1] = m
                    showEndTimePicker = false
                },
                onDismiss = { showEndTimePicker = false }
            )
        } else {
            DialTimePickerDialog(
                initialHour = endHour,
                initialMinute = endMinute,
                onConfirm = { h, m ->
                    endHour = h
                    endMinute = m
                    endRef[0] = h
                    endRef[1] = m
                    showEndTimePicker = false
                },
                onDismiss = { showEndTimePicker = false }
            )
        }
    }
}

@Composable
fun WheelTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedHour, selectedMinute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                TimeWheelPicker(
                    initialHour = selectedHour,
                    initialMinute = selectedMinute,
                    onTimeChanged = { h, m ->
                        selectedHour = h
                        selectedMinute = m
                    }
                )
            }
        },
        containerColor = Color(0xFF1C1C1E),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
fun TimeDisplayRow(label: String, hour: Int, minute: Int, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
        val amPm = if (hour >= 12) "PM" else "AM"
        val hour12 = if (hour % 12 == 0) 12 else hour % 12
        Text(
            text = String.format("%02d:%02d %s", hour12, minute, amPm),
            style = MaterialTheme.typography.displayLarge,
            color = Color.White,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

@Composable
fun OptionItem(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, color = Color.White, fontSize = 16.sp)
            if (value.isNotEmpty()) {
                Text(value, color = Color(0xFF448AFF), fontSize = 14.sp)
            }
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
fun OptionItemToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 16.sp)
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF448AFF),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

fun getRepeatSummary(days: Set<Int>): String {
    if (days.isEmpty()) return "Once"
    if (days.size == 7) return "Every day"
    if (days.containsAll(listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)) && days.size == 5) return "Weekdays"
    // TODO: Better formatting (e.g. "Mon, Tue")
    return "Custom"
}

fun getRingtoneName(context: android.content.Context, uriString: String?): String {
    if (uriString == null) return "Default"
    val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uriString))
    return ringtone?.getTitle(context) ?: "Unknown"
}

@Composable
fun TimeWheelPicker(
    initialHour: Int,
    initialMinute: Int,
    onTimeChanged: (Int, Int) -> Unit
) {
    // We use AndroidView to wrap NumberPickers
    // We notify on change
    
    // Logic to convert 24h -> 12h for display
    val amPmInit = if (initialHour >= 12) 1 else 0 // 0=AM, 1=PM
    val hour12Init = if (initialHour % 12 == 0) 12 else initialHour % 12
    
    var selectedHour12 by remember { mutableStateOf(hour12Init) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    var selectedAmPm by remember { mutableStateOf(amPmInit) }
    
    val updateTime = {
        val hour24 = if (selectedAmPm == 0) { // AM
             if (selectedHour12 == 12) 0 else selectedHour12
        } else { // PM
             if (selectedHour12 == 12) 12 else selectedHour12 + 12
        }
        onTimeChanged(hour24, selectedMinute)
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hour Picker
        AndroidView(
            modifier = Modifier.width(80.dp),
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = 1
                    maxValue = 12
                    value = hour12Init
                    setTextColorCompat(this, 0xFFFFFFFF.toInt())
                    setOnValueChangedListener { _, _, newVal ->
                        selectedHour12 = newVal
                        updateTime()
                    }
                }
            }
        )
        
        // Minute Picker
        AndroidView(
            modifier = Modifier.width(80.dp),
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = 0
                    maxValue = 59
                    value = initialMinute
                    setFormatter { i -> String.format("%02d", i) }
                    setTextColorCompat(this, 0xFFFFFFFF.toInt())
                    setOnValueChangedListener { _, _, newVal ->
                        selectedMinute = newVal
                        updateTime()
                    }
                }
            }
        )
        
        // AM/PM Picker
        AndroidView(
            modifier = Modifier.width(80.dp),
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = 0
                    maxValue = 1
                    displayedValues = arrayOf("AM", "PM")
                    value = amPmInit
                    setTextColorCompat(this, 0xFFFFFFFF.toInt())
                    setOnValueChangedListener { _, _, newVal ->
                        selectedAmPm = newVal
                        updateTime()
                    }
                }
            }
        )
    }
}

// Reflection hack to change NumberPicker text color (Standard Android API doesn't expose it easily)
fun setTextColorCompat(picker: NumberPicker, color: Int) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        picker.textColor = color
    } else {
        // Fallback for older versions if needed using reflection (omitted for safety in this strict env)
    }
}
