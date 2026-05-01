package com.example.reversealarm.ui.screens

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reversealarm.data.UserPreferencesRepository
import com.example.reversealarm.services.LockOverlayService
import com.example.reversealarm.util.AlarmScheduler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    userPreferences: UserPreferencesRepository,
    alarmRepository: com.example.reversealarm.data.AlarmRepository,
    onNavigateToSchedule: (Int?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val alarms by alarmRepository.allAlarms.collectAsState(initial = emptyList())
    val selectedAlarmIds = remember { mutableStateListOf<Int>() }
    val isSelectionMode = selectedAlarmIds.isNotEmpty()
    
    Scaffold(
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { onNavigateToSchedule(null) }, // Add new
                    containerColor = Color(0xFF448AFF),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Schedule", tint = Color.White)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedAlarmIds.clear() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                        }
                        Text(
                            "${selectedAlarmIds.size} Selected", 
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    IconButton(onClick = {
                        scope.launch {
                            val alarmsToDelete = alarms.filter { it.id in selectedAlarmIds }
                            alarmsToDelete.forEach { 
                                AlarmScheduler.cancelAlarm(context, it.id)
                                alarmRepository.delete(it)
                            }
                            selectedAlarmIds.clear()
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = Color(0xFFD32F2F))
                    }
                } else {
                    Text(
                        "Reverse Alarm", 
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Debug / Test
                    TextButton(onClick = {
                        val intent = Intent(context, LockOverlayService::class.java)
                        intent.action = LockOverlayService.ACTION_START_LOCK
                        intent.putExtra(LockOverlayService.EXTRA_DURATION_MILLIS, 10000L)
                        context.startService(intent)
                    }) {
                        Text("TEST v2", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
            
            val activeCount = alarms.count { it.isArmed }
            if (!isSelectionMode) {
                Text(
                    text = if (activeCount > 0) "$activeCount active alarms" else "All alarms turned off",
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alarms.size) { index ->
                    val alarm = alarms[index]
                    val isSelected = selectedAlarmIds.contains(alarm.id)
                    
                    AlarmCard(
                        alarm = alarm,
                        isSelected = isSelected,
                        selectionMode = isSelectionMode,
                        onToggle = { isChecked ->
                            scope.launch {
                                val updated = alarm.copy(isArmed = isChecked)
                                alarmRepository.update(updated)
                                if (isChecked) {
                                    AlarmScheduler.scheduleAlarm(context, updated)
                                } else {
                                    AlarmScheduler.cancelAlarm(context, updated.id)
                                }
                            }
                        },
                        onClick = {
                            if (isSelectionMode) {
                                if (isSelected) selectedAlarmIds.remove(alarm.id)
                                else selectedAlarmIds.add(alarm.id)
                            } else {
                                onNavigateToSchedule(alarm.id)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                selectedAlarmIds.add(alarm.id)
                            }
                        }
                    )
                }
                
                if (alarms.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No active schedule. Tap + to create one.",
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmCard(
    alarm: com.example.reversealarm.data.Alarm,
    isSelected: Boolean,
    selectionMode: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2C2C2E) else Color(0xFF1C1C1E)
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF448AFF)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Formatting Time
                val startAmPm = if (alarm.startHour >= 12) "PM" else "AM"
                val startH12 = if (alarm.startHour == 0) 12 else if (alarm.startHour > 12) alarm.startHour - 12 else alarm.startHour
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format("%02d:%02d", startH12, alarm.startMinute),
                        fontSize = 36.sp,
                        color = if (alarm.isArmed) Color.White else Color.Gray,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = startAmPm,
                        fontSize = 16.sp,
                        color = if (alarm.isArmed) Color.White else Color.Gray,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                
                // Summary
                val daysSummary = if (alarm.repeatDays.isEmpty()) "Once" else "Custom" // Simplified for now
                // We should add a real formatter but let's stick to basic
                Text(
                    text = daysSummary + " | Ends ${String.format("%02d:%02d", alarm.endHour, alarm.endMinute)}",
                    color = if (alarm.isArmed) Color.Gray else Color.DarkGray,
                    fontSize = 14.sp
                )
            }
            
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF448AFF),
                        uncheckedColor = Color.Gray,
                        checkmarkColor = Color.White
                    )
                )
            } else {
                Switch(
                    checked = alarm.isArmed,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF448AFF),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
            }
        }
    }
}
