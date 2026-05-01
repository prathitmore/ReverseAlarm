package com.example.reversealarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import com.example.reversealarm.services.RestrictionAccessibilityService
import com.example.reversealarm.ui.screens.HomeScreen
import com.example.reversealarm.ui.screens.PermissionScreen
import com.example.reversealarm.ui.screens.LockdownScheduleScreen
import com.example.reversealarm.data.UserPreferencesRepository
import com.example.reversealarm.ui.theme.ReverseAlarmTheme
import com.example.reversealarm.util.PermissionManager
import com.example.reversealarm.data.AlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var alarmRepository: AlarmRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReverseAlarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(alarmRepository)
                }
            }
        }
    }
}



@Composable
fun MainNavigation(alarmRepository: AlarmRepository) {
    val context = LocalContext.current
    val lifeCycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    
    // Initial check
    var hasOverlay by remember { mutableStateOf(PermissionManager.hasOverlayPermission(context)) }
    var hasAccessibility by remember { mutableStateOf(PermissionManager.hasAccessibilityPermission(context, RestrictionAccessibilityService::class.java)) }
    var hasDeviceAdmin by remember { mutableStateOf(PermissionManager.hasDeviceAdminPermission(context)) }
    var hasBatteryOptimization by remember { mutableStateOf(PermissionManager.isBatteryOptimizationDisabled(context)) }
    var hasNotificationPermission by remember { mutableStateOf(PermissionManager.hasNotificationPermission(context)) }
    var hasAlarmPermission by remember { mutableStateOf(PermissionManager.hasExactAlarmPermission(context)) }

    DisposableEffect(lifeCycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasOverlay = PermissionManager.hasOverlayPermission(context)
                hasAccessibility = PermissionManager.hasAccessibilityPermission(context, RestrictionAccessibilityService::class.java)
                hasDeviceAdmin = PermissionManager.hasDeviceAdminPermission(context)
                hasBatteryOptimization = PermissionManager.isBatteryOptimizationDisabled(context)
                hasNotificationPermission = PermissionManager.hasNotificationPermission(context)
                hasAlarmPermission = PermissionManager.hasExactAlarmPermission(context)
            }
        }
        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var currentScreen by remember { mutableStateOf("home") } // "home", "schedule"
    var editingAlarmId by remember { mutableStateOf<Int?>(null) }
    
    // Separate state for Bottom Nav selection
    // 0 = Home, 1 = Info
    var selectedBottomIndex by remember { mutableIntStateOf(0) }

    val userPrefs = remember { UserPreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    var backPressCount by remember { mutableIntStateOf(0) }
    var hasSeenTutorial by remember { mutableStateOf<Boolean?>(null) }
    var forceShowTutorial by remember { mutableStateOf(false) }
    var tutorialReplayCounter by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        userPrefs.hasSeenTutorial.collect { seen ->
            hasSeenTutorial = seen
        }
    }

    LaunchedEffect(currentScreen) {
        backPressCount = 0
    }

    androidx.activity.compose.BackHandler(enabled = true) {
        if (currentScreen == "schedule") {
            currentScreen = "home"
        } else {
            if (backPressCount == 0) {
                backPressCount++
                android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
                scope.launch {
                    kotlinx.coroutines.delay(2000)
                    backPressCount = 0
                }
            } else {
                (context as? android.app.Activity)?.finish()
            }
        }
    }

    if (hasSeenTutorial == null) {
        // wait
    } else if (hasOverlay && hasAccessibility && hasDeviceAdmin && hasBatteryOptimization && hasNotificationPermission && hasAlarmPermission) {
        
        Box(modifier = Modifier.fillMaxSize()) {
            if (currentScreen == "schedule") {
             LockdownScheduleScreen(
                 userPreferences = userPrefs,
                 alarmRepository = alarmRepository,
                 alarmId = editingAlarmId,
                 isEditMode = editingAlarmId != null,
                 onBack = { currentScreen = "home" } 
             )
        } else {
            // Main flow with Bottom Nav
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF1C1C1E),
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ) {
                        NavigationBarItem(
                            icon = { Icon(androidx.compose.material.icons.Icons.Filled.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            selected = selectedBottomIndex == 0,
                            onClick = { selectedBottomIndex = 0 },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = androidx.compose.ui.graphics.Color(0xFF448AFF),
                                selectedIconColor = androidx.compose.ui.graphics.Color.White,
                                selectedTextColor = androidx.compose.ui.graphics.Color.White,
                                unselectedIconColor = androidx.compose.ui.graphics.Color.Gray,
                                unselectedTextColor = androidx.compose.ui.graphics.Color.Gray
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(androidx.compose.material.icons.Icons.Filled.Info, contentDescription = "Info") },
                            label = { Text("Info") },
                            selected = selectedBottomIndex == 1,
                            onClick = { selectedBottomIndex = 1 },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = androidx.compose.ui.graphics.Color(0xFF448AFF),
                                selectedIconColor = androidx.compose.ui.graphics.Color.White,
                                selectedTextColor = androidx.compose.ui.graphics.Color.White,
                                unselectedIconColor = androidx.compose.ui.graphics.Color.Gray,
                                unselectedTextColor = androidx.compose.ui.graphics.Color.Gray
                            )
                        )
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    if (selectedBottomIndex == 0) {
                        HomeScreen(
                            userPreferences = userPrefs, 
                            alarmRepository = alarmRepository,
                            onNavigateToSchedule = { alarmId -> 
                                editingAlarmId = alarmId
                                currentScreen = "schedule" 
                            }
                        )
                    } else {
                        com.example.reversealarm.ui.screens.InfoScreen(
                            onReplayTutorial = { 
                                selectedBottomIndex = 0
                                currentScreen = "home"
                                tutorialReplayCounter++
                                forceShowTutorial = true 
                            },
                            onClearData = {
                                scope.launch {
                                    alarmRepository.deleteAll()
                                    selectedBottomIndex = 0
                                    currentScreen = "home"
                                    tutorialReplayCounter++
                                    forceShowTutorial = true
                                }
                            }
                        )
                    }
                }
            }
        }
            
            if ((hasSeenTutorial == false || forceShowTutorial) && hasOverlay && hasAccessibility && hasDeviceAdmin) {
                androidx.compose.runtime.key(tutorialReplayCounter) {
                    com.example.reversealarm.ui.screens.TutorialOverlay(
                        currentScreen = currentScreen,
                        selectedTab = selectedBottomIndex,
                        onFinish = {
                            forceShowTutorial = false
                            currentScreen = "home"
                            if (hasSeenTutorial == false) {
                                scope.launch { userPrefs.setHasSeenTutorial(true) }
                            }
                        }
                    )
                }
            }
        }
    } else {
        PermissionScreen(
            hasOverlay = hasOverlay,
            hasAccessibility = hasAccessibility,
            hasDeviceAdmin = hasDeviceAdmin,
            hasBatteryOptimization = hasBatteryOptimization,
            hasNotificationPermission = hasNotificationPermission,
            hasAlarmPermission = hasAlarmPermission,
            onContinue = {
                hasOverlay = PermissionManager.hasOverlayPermission(context)
                hasAccessibility = PermissionManager.hasAccessibilityPermission(context, RestrictionAccessibilityService::class.java)
                hasDeviceAdmin = PermissionManager.hasDeviceAdminPermission(context)
                hasBatteryOptimization = PermissionManager.isBatteryOptimizationDisabled(context)
                hasNotificationPermission = PermissionManager.hasNotificationPermission(context)
                hasAlarmPermission = PermissionManager.hasExactAlarmPermission(context)
            }
        )
    }
}
