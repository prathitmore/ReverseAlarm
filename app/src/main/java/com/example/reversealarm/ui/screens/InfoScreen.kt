package com.example.reversealarm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import com.example.reversealarm.data.AlarmRepository

@Composable
fun InfoScreen(onReplayTutorial: () -> Unit, onClearData: () -> Unit) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            text = "About Reverse Alarm",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                InfoSection(
                    title = "What is this?",
                    content = "Reverse Alarm is a negative reinforcement tool designed to force you to stop using your phone at night. Instead of waking you up, it locks you out."
                )
            }

            item {
                InfoSection(
                    title = "How it works",
                    content = "1. Set a 'Lock Start' and 'Lock End' time.\n" +
                            "2. When the time comes, a system-level overlay blocks your screen.\n" +
                            "3. The only way to stop it is to put your phone down and go to sleep."
                )
            }

            item {
                InfoSection(
                    title = "Permissions",
                    content = "This app requires several sensitive permissions to work effectively:\n" +
                            "• Overlay: To block the screen.\n" +
                            "• Accessibility: To prevent you from closing the app.\n" +
                            "• Device Admin: To prevent you from uninstalling it during a lockdown."
                )
            }


            item {
                Button(
                    onClick = onReplayTutorial,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF448AFF))
                ) {
                    Text("Replay Tutorial", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                ) {
                    Text("Clear App Data & Start Fresh", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }


        }
        
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Clear All Data?", color = Color.White) },
                text = { Text("This will permanently delete all your alarms and schedules. Do you want to continue?", color = Color.LightGray) },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirmDialog = false
                        onClearData() // Trigger delegate handles wipe, nav, and tutorial
                    }) {
                        Text("Yes, Delete All", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1C1C1E)
            )
        }
    }
}

@Composable
fun InfoSection(title: String, content: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF448AFF),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                lineHeight = 20.sp
            )
        }
    }
}
