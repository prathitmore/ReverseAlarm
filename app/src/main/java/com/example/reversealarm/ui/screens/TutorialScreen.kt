package com.example.reversealarm.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TutorialOverlay(
    currentScreen: String,
    selectedTab: Int,
    onFinish: () -> Unit
) {
    var scheduleStep by remember { mutableIntStateOf(0) }
    var returnedHome by remember { mutableStateOf(false) }
    
    val config = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { config.screenWidthDp.dp.toPx() }
    val screenHeight = with(LocalDensity.current) { config.screenHeightDp.dp.toPx() }
    
    // Compute current logical step based on app state
    val step = when {
        currentScreen == "home" && selectedTab == 0 && !returnedHome -> 0
        currentScreen == "schedule" && scheduleStep == 0 -> 1
        currentScreen == "schedule" && scheduleStep == 1 -> 2
        currentScreen == "schedule" && scheduleStep >= 2 -> 3
        currentScreen == "home" && selectedTab == 0 && returnedHome -> 4
        else -> -1
    }

    LaunchedEffect(currentScreen) {
        if (currentScreen == "home" && scheduleStep > 0) {
            returnedHome = true
        }
    }
    
    // Animate alpha for smooth transitions between steps
    val alphaAnim by animateFloatAsState(targetValue = 1f, animationSpec = tween(300))
    
    if (step == -1) return

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        
        // Full screen dimmed background with cutout
        // No pointerInput means touches pass entirely through the transparent layout!
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // Draw dim layer
            drawRect(
                color = Color.Black.copy(alpha = 0.75f),
                size = size
            )
            
            when (step) {
                0 -> {
                    // Cutout for FAB (bottom center)
                    drawCircle(
                        color = Color.Transparent,
                        radius = 120f,
                        center = Offset(canvasWidth / 2f, canvasHeight - 280f),
                        blendMode = BlendMode.Clear
                    )
                }
                1 -> {
                    // Cutout for Schedule Time pickers (Top area)
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(40f, 150f),
                        size = Size(canvasWidth - 80f, 850f),
                        cornerRadius = CornerRadius(40f, 40f),
                        blendMode = BlendMode.Clear
                    )
                }
                2 -> {
                    // Cutout for Restrictions (Bottom area of schedule config)
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(40f, 1050f),
                        size = Size(canvasWidth - 80f, 700f),
                        cornerRadius = CornerRadius(40f, 40f),
                        blendMode = BlendMode.Clear
                    )
                }
                3 -> {
                    // Cutout for Save Button (Top Right)
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(canvasWidth - 180f, 30f),
                        size = Size(180f, 120f),
                        cornerRadius = CornerRadius(20f, 20f),
                        blendMode = BlendMode.Clear
                    )
                }
                4 -> {
                    // Cutout for first alarm card (on Home Screen)
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(40f, 290f),
                        size = Size(canvasWidth - 80f, 300f),
                        cornerRadius = CornerRadius(40f, 40f),
                        blendMode = BlendMode.Clear
                    )
                }
            }
        }

        // Skip Button
        TextButton(
            onClick = onFinish,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Text("Skip", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // Tooltip placement logic
        when (step) {
            0 -> {
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp).padding(horizontal = 24.dp)) {
                    PremiumTooltip(
                        text = "🚀 Welcome to Reverse Alarm!\nLet's create an unbreakable lockdown schedule. We'll walk you through creating a test schedule.\n\n👉 Real interaction enabled! Tap the underlying + button to begin.",
                        step = 0,
                        totalSteps = 5,
                        pointerDirection = "Down",
                        hasActionBtn = false,
                        onNext = {}
                    )
                }
            }
            1 -> {
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp).padding(horizontal = 24.dp)) {
                    PremiumTooltip(
                        text = "🕰️ Scheduling\nWe've automatically pre-filled your time to be exactly 5 minutes from now, with a 1 minute duration. Feel free to adjust it using the real dials!\n\nTap NEXT to review restrictions.",
                        step = 1,
                        totalSteps = 5,
                        pointerDirection = "Up", // Below the cutout
                        hasActionBtn = true,
                        onNext = { scheduleStep++ }
                    )
                }
            }
            2 -> {
                Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 180.dp).padding(horizontal = 24.dp)) {
                    PremiumTooltip(
                        text = "🔒 Restrictions Check\nEvery feature you enable here will be aggressively blocked. Ensure you choose the right restrictions for your goal!\n\nTap NEXT to continue.",
                        step = 2,
                        totalSteps = 5,
                        pointerDirection = "Down", // Above the cutout
                        hasActionBtn = true,
                        onNext = { scheduleStep++ }
                    )
                }
            }
            3 -> {
                Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 150.dp).padding(horizontal = 24.dp)) {
                    PremiumTooltip(
                        text = "⚠️ WARNING!\nWhen you tap save, the alarm locks in. You CANNOT cancel it during its active period. Put your phone away!\n\n👉 Tap the REAL Save button at the top to arm it.",
                        step = 3,
                        totalSteps = 5,
                        pointerDirection = "Up",
                        hasActionBtn = false,
                        onNext = {}
                    )
                }
            }
            4 -> {
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 200.dp).padding(horizontal = 24.dp)) {
                    PremiumTooltip(
                        text = "✅ Active Schedule\nYour test alarm is safely scheduled to execute! Switch it off now if you do not want to go into lockdown.",
                        step = 4,
                        totalSteps = 5,
                        pointerDirection = "Up",
                        hasActionBtn = true,
                        onNext = onFinish
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumTooltip(
    text: String,
    step: Int,
    totalSteps: Int,
    pointerDirection: String,
    hasActionBtn: Boolean,
    onNext: () -> Unit
) {
    val boxColor = Color(0xFF161619) // Very Dark elegant
    
    Box {
        // We draw the unified shape
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val radius = 48f
            val pointerSize = 36f
            
            val path = Path().apply {
                if (pointerDirection == "Down") {
                    addRoundRect(RoundRect(0f, 0f, w, h - pointerSize, CornerRadius(radius, radius)))
                    moveTo(w / 2f - pointerSize, h - pointerSize)
                    lineTo(w / 2f, h)
                    lineTo(w / 2f + pointerSize, h - pointerSize)
                } else {
                    addRoundRect(RoundRect(0f, pointerSize, w, h, CornerRadius(radius, radius)))
                    moveTo(w / 2f - pointerSize, pointerSize)
                    lineTo(w / 2f, 0f)
                    lineTo(w / 2f + pointerSize, pointerSize)
                }
                close()
            }
            
            drawPath(path, color = boxColor)
            
            // Subtle glowing border or shadow could be added here
        }
        
        // Inner Content matching exact visual style
        val pt = if (pointerDirection == "Up") 24.dp else 0.dp
        val pb = if (pointerDirection == "Down") 24.dp else 0.dp
        
        Column(
            modifier = Modifier.padding(top = 24.dp + pt, bottom = 24.dp + pb, start = 24.dp, end = 24.dp)
        ) {
            Text(
                text = text,
                color = Color(0xFFE0E0E0),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until totalSteps) {
                        if (i == step) {
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height(6.dp)
                                    .background(Color.White, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.DarkGray, CircleShape)
                            )
                        }
                    }
                }
                
                if (hasActionBtn) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, CircleShape)
                            .clickable { onNext() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Next Hint",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
