package com.example.distortiontracker.ui.main

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.distortiontracker.data.DistortionManager
import com.example.distortiontracker.theme.NeonCrimson
import kotlinx.coroutines.flow.StateFlow

// Frosted glass card background color
private val GlassBackground = Color(0x60404859)
private val GlassBorder = Color(0x30FFFFFF)

@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        SpaceBackground(
            currentDistortionIndex = if (state.isCalibrated) state.currentDistortionIndex else 0,
            nextDistortionIndex = if (state.isCalibrated) state.nextDistortionIndex else 1
        )

        if (!state.isCalibrated) {
            CalibrationScreen(
                onCalibrate = { index -> viewModel.calibrate(index) }
            )
        } else {
            DistortionDashboard(
                state = state,
                timeRemainingFlow = viewModel.timeRemaining,
                onSetTarget = { index, is5Min -> viewModel.setTarget(index, is5Min) }
            )
        }
    }
}

@Composable
fun CalibrationScreen(onCalibrate: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "DISTORTION TRACKER",
            color = Color.Black,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Glowing Red Ring
        Box(
            modifier = Modifier
                .size(160.dp)
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            GlowingRing(size = 120f)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "CALIBRATION REQUIRED",
            color = Color.Black,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Select the active in-game Distortion to calibrate the tracker.",
            color = Color.Black,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Destination buttons as frosted glass cards
        DistortionManager.DESTINATIONS.forEachIndexed { index, name ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onCalibrate(index) },
                contentAlignment = Alignment.Center
            ) {
                GlassLayer(
                    shape = RoundedCornerShape(12.dp),
                    color = GlassBackground
                )
                Text(
                    text = name,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun DistortionDashboard(
    state: DistortionUiState,
    timeRemainingFlow: StateFlow<Long>,
    onSetTarget: (Int, Boolean) -> Unit
) {
    val context = LocalContext.current
    var showWarningDialog by remember { mutableStateOf(false) }
    var warningType by remember { mutableStateOf("") } // "5min" or "20min"
    var pendingTargetIndex by remember { mutableStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: "DISTORTION TRACKER"
        Text(
            text = "DISTORTION TRACKER",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 8.dp, bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassLayer(
                shape = DestinationCardShape(80.dp),
                color = GlassBackground
            )
            
            // Glowing Red Ring in the center
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                GlowingRing(size = 140f)
                Text(
                    text = state.currentDistortion,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ═══════════════════════════════════════
        // TIMER CARD
        // ═══════════════════════════════════════
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            GlassLayer(
                shape = RoundedCornerShape(12.dp),
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x55E53935),
                        GlassBackground,
                        Color(0x55E53935)
                    )
                )
            )
            Box(modifier = Modifier.padding(vertical = 20.dp)) {
                CountdownTimer(timeRemainingFlow = timeRemainingFlow)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ═══════════════════════════════════════
        // UP NEXT BAR
        // ═══════════════════════════════════════
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassLayer(
                shape = RoundedCornerShape(8.dp),
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x40E53935),
                        GlassBackground
                    )
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UP NEXT: ",
                    color = Color(0xFFB0BEC5),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = state.nextDistortion,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ═══════════════════════════════════════
        // ACTIVE DISTORTIONS - Expandable List
        // ═══════════════════════════════════════
        var expanded by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassLayer(
                shape = RoundedCornerShape(12.dp),
                color = GlassBackground,
                borderColor = GlassBorder
            )
            Column {
                // Header row (clickable to toggle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rotating arrowhead icon
                    val arrowRotation by animateFloatAsState(
                        targetValue = if (expanded) 90f else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "arrowRotation"
                    )
                    Canvas(modifier = Modifier.size(16.dp)) {
                        withTransform({
                            rotate(arrowRotation, Offset(size.width / 2f, size.height / 2f))
                        }) {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(0f, size.height * 0.15f)
                                lineTo(size.width * 0.9f, size.height / 2f)
                                lineTo(0f, size.height * 0.85f)
                                lineTo(size.width * 0.25f, size.height / 2f)
                                close()
                            }
                            drawPath(path, NeonCrimson)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "ACTIVE DISTORTIONS",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Expandable destination list (visually nested inside)
                AnimatedVisibility(visible = expanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    ) {
                        GlassLayer(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x600E1116),
                            borderColor = Color(0x15FFFFFF)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .drawBehind {
                                    val dotCenterX = (4.dp).toPx()
                                    // Total number of items is 7. Each item is spaced equally.
                                    val itemHeight = size.height / 7f
                                    val startY = itemHeight / 2f
                                    val endY = size.height - (itemHeight / 2f)
                                    drawLine(
                                        color = NeonCrimson.copy(alpha = 0.4f),
                                        start = Offset(dotCenterX, startY),
                                        end = Offset(dotCenterX, endY),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                }
                        ) {
                            val currentIndex = state.currentDistortionIndex
                            val nextIndex = state.nextDistortionIndex

                            DistortionManager.DESTINATIONS.forEachIndexed { index, name ->
                                val suffix = when (index) {
                                    currentIndex -> " (Current)"
                                    nextIndex -> " (Next)"
                                    else -> ""
                                }
                                val isSelected = state.targetDistortionIndex == index
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (index == currentIndex) return@clickable // Can't notify for current

                                            // Calculate time until this distortion
                                            var hourDiff = index - currentIndex
                                            if (hourDiff <= 0) hourDiff += 7

                                            val timeRemainingMillis = DistortionManager.getTimeRemainingMillis()
                                            val timeUntilTargetMillis =
                                                timeRemainingMillis + ((hourDiff - 1) * 60 * 60 * 1000L)
                                            val minsUntil = timeUntilTargetMillis / (1000 * 60)

                                            if (minsUntil < 5) {
                                                warningType = "5min"
                                                showWarningDialog = true
                                            } else if (minsUntil < 20) {
                                                pendingTargetIndex = index
                                                warningType = "20min"
                                                showWarningDialog = true
                                            } else {
                                                onSetTarget(index, false)
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Selection dot
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) NeonCrimson
                                                else Color(0xFF555555)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "$name$suffix",
                                        color = if (index == currentIndex) NeonCrimson
                                        else if (isSelected) Color.White
                                        else Color(0xFFB0BEC5),
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ═══════════════════════════════════════
        // NOTIFICATION TOGGLE
        // ═══════════════════════════════════════
        val notificationsEnabled = state.targetDistortionIndex >= 0

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassLayer(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x801A1D24),
                borderColor = Color(0x20FFFFFF)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Bell icon drawn with Canvas
                Canvas(modifier = Modifier.size(20.dp)) {
                    val bellColor = NeonCrimson
                    // Bell body
                    drawCircle(bellColor, radius = size.width * 0.35f, center = Offset(size.width / 2f, size.height * 0.4f))
                    // Bell base
                    drawRect(bellColor, topLeft = Offset(size.width * 0.2f, size.height * 0.4f), size = androidx.compose.ui.geometry.Size(size.width * 0.6f, size.height * 0.3f))
                    // Clapper
                    drawCircle(bellColor, radius = size.width * 0.1f, center = Offset(size.width / 2f, size.height * 0.85f))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "SET NOTIFICATIONS",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            onSetTarget(-1, false) // Disable
                        }
                        // If enabling, user picks from the list above
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonCrimson,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF333333)
                    )
                )
            }
        }

        if (notificationsEnabled) {
            Text(
                text = "Notifying ${if (state.is5MinWarning) 5 else 20} min before: ${DistortionManager.DESTINATIONS[state.targetDistortionIndex]}",
                color = Color(0xFF888888),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // ═══════════════════════════════════════
    // WARNING DIALOGS
    // ═══════════════════════════════════════
    if (showWarningDialog && warningType == "5min") {
        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            title = { Text("TOO LATE!") },
            text = { Text("Why the hell are you still here? Get in the game! It starts in less than 5 minutes!") },
            confirmButton = {
                TextButton(onClick = { showWarningDialog = false }) {
                    Text("ON MY WAY", color = NeonCrimson, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1A1D24),
            titleContentColor = NeonCrimson,
            textContentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showWarningDialog && warningType == "20min") {
        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            title = { Text("Starting Soon") },
            text = { Text("This distortion starts in less than 20 minutes. Would you like a 5-minute early warning instead?") },
            confirmButton = {
                TextButton(onClick = {
                    onSetTarget(pendingTargetIndex, true)
                    showWarningDialog = false
                }) {
                    Text("YES, 5 MINS", color = NeonCrimson, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarningDialog = false }) {
                    Text("CANCEL", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1A1D24),
            titleContentColor = NeonCrimson,
            textContentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ═══════════════════════════════════════════════
// GLOWING RED RING — the signature distortion icon
// ═══════════════════════════════════════════════
@Composable
fun GlowingRing(size: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "riftAnimation")
    
    // Heartbeat scale animation: quick expansion, quick contraction, then rest
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                1.0f at 0
                1.08f at 150 using FastOutSlowInEasing
                0.98f at 350 using FastOutSlowInEasing
                1.03f at 500 using FastOutSlowInEasing
                1.0f at 700
                1.0f at 2000
            }
        ),
        label = "riftScale"
    )

    // Continuous noise/wobble phase animation
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing)
        ),
        label = "riftPhase"
    )

    // Rotation of outer wisps
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing)
        ),
        label = "riftRotation"
    )

    val densityLocal = LocalDensity.current
    
    // Cache stroke allocations to prevent object creation during draw execution
    val thickGlowStroke = remember(densityLocal) { Stroke(width = with(densityLocal) { 12.dp.toPx() }, join = StrokeJoin.Round) }
    val mediumGlowStroke = remember(densityLocal) { Stroke(width = with(densityLocal) { 5.dp.toPx() }, join = StrokeJoin.Round) }
    val coreStroke = remember(densityLocal) { Stroke(width = with(densityLocal) { 2.dp.toPx() }, join = StrokeJoin.Round) }
    val wispStroke1 = remember(densityLocal) { Stroke(width = with(densityLocal) { 2.dp.toPx() }, join = StrokeJoin.Round) }
    val wispStroke2 = remember(densityLocal) { Stroke(width = with(densityLocal) { 1.5.dp.toPx() }, join = StrokeJoin.Round) }

    // Cache gradient color lists
    val riftColors = remember { listOf(Color(0xFF230835), Color(0xFF0F0419), Color(0xFF020005)) }
    val vortexColors = remember { listOf(Color(0x35E53935), Color.Transparent, Color(0x35311B92)) }
    val coronaColors = remember { listOf(NeonCrimson.copy(alpha = 0.45f), NeonCrimson.copy(alpha = 0.15f), Color.Transparent) }

    // Reuse Path objects across drawing frames
    val clipPath = remember { androidx.compose.ui.graphics.Path() }
    val mainPath = remember { androidx.compose.ui.graphics.Path() }
    val wispPath1 = remember { androidx.compose.ui.graphics.Path() }
    val wispPath2 = remember { androidx.compose.ui.graphics.Path() }

    val density = densityLocal.density
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = (size / 2f) * density
        
        // 1. Draw the "different dimension" inside the circular rift
        // We clip to the circle and draw a swirling vortex
        drawContext.canvas.save()
        clipPath.reset()
        clipPath.addOval(androidx.compose.ui.geometry.Rect(center, radius))
        drawContext.canvas.clipPath(clipPath)
        
        // Draw the rift interior: dark purple/cyan void
        drawRect(
            brush = Brush.radialGradient(
                colors = riftColors,
                center = center,
                radius = radius
            )
        )
        
        // Swirling vortex texture inside
        withTransform({
            rotate(rotation * 1.5f, center)
        }) {
            drawCircle(
                brush = Brush.linearGradient(
                    colors = vortexColors,
                    start = Offset(center.x - radius, center.y - radius),
                    end = Offset(center.x + radius, center.y + radius)
                ),
                radius = radius * 0.9f,
                center = center
            )
        }
        
        // Restore canvas from clipping
        drawContext.canvas.restore()

        // 2. Glowing background overlay (corona)
        drawCircle(
            brush = Brush.radialGradient(
                colors = coronaColors,
                center = center,
                radius = radius * 1.35f * scale
            ),
            radius = radius * 1.35f * scale,
            center = center
        )

        // 3. Draw the main jagged rift path
        mainPath.reset()
        val numPoints = 80
        val currentRadius = radius * scale
        val piFloat = Math.PI.toFloat()
        
        for (i in 0..numPoints) {
            val angle = (i.toFloat() / numPoints) * 2f * piFloat
            // Procedural noise using phase and different frequencies for organic crack look
            val noise = kotlin.math.sin(angle * 8f + phase * 2f) * 0.04f +
                        kotlin.math.cos(angle * 16f - phase) * 0.02f +
                        kotlin.math.sin(angle * 28f + phase * 3f) * 0.01f
            
            val r = currentRadius * (1f + noise)
            val x = center.x + r * kotlin.math.cos(angle)
            val y = center.y + r * kotlin.math.sin(angle)
            
            if (i == 0) {
                mainPath.moveTo(x, y)
            } else {
                mainPath.lineTo(x, y)
            }
        }
        mainPath.close()

        // Draw multiple layers of the crack for a high-intensity glow
        drawPath(path = mainPath, color = NeonCrimson.copy(alpha = 0.35f), style = thickGlowStroke)
        drawPath(path = mainPath, color = NeonCrimson.copy(alpha = 0.75f), style = mediumGlowStroke)
        drawPath(path = mainPath, color = Color(0xFFFFCDD2), style = coreStroke)

        // 4. Energetic wisps (arcs trailing around the rift)
        withTransform({
            rotate(rotation, center)
        }) {
            // Draw 2-3 glowing wisps outside the ring
            val wispRadius1 = currentRadius * 1.08f
            wispPath1.reset()
            val startAngle = 0f
            val endAngle = piFloat / 3f
            val steps = 15
            for (j in 0..steps) {
                val angle = startAngle + (j.toFloat() / steps) * (endAngle - startAngle)
                val wispNoise = kotlin.math.sin(angle * 10f + phase) * 0.02f
                val r = wispRadius1 * (1f + wispNoise)
                val x = center.x + r * kotlin.math.cos(angle)
                val y = center.y + r * kotlin.math.sin(angle)
                if (j == 0) wispPath1.moveTo(x, y) else wispPath1.lineTo(x, y)
            }
            drawPath(path = wispPath1, color = NeonCrimson.copy(alpha = 0.6f), style = wispStroke1)

            val wispRadius2 = currentRadius * 0.92f
            wispPath2.reset()
            val startAngle2 = piFloat
            val endAngle2 = piFloat * 1.4f
            for (j in 0..steps) {
                val angle = startAngle2 + (j.toFloat() / steps) * (endAngle2 - startAngle2)
                val wispNoise = kotlin.math.cos(angle * 12f - phase) * 0.02f
                val r = wispRadius2 * (1f + wispNoise)
                val x = center.x + r * kotlin.math.cos(angle)
                val y = center.y + r * kotlin.math.sin(angle)
                if (j == 0) wispPath2.moveTo(x, y) else wispPath2.lineTo(x, y)
            }
            drawPath(path = wispPath2, color = NeonCrimson.copy(alpha = 0.5f), style = wispStroke2)
        }
    }
}

// ═══════════════════════════════════════════════
// ISOLATED COUNTDOWN TIMER (prevents recomposition of other UI)
// ═══════════════════════════════════════════════
@Composable
fun CountdownTimer(timeRemainingFlow: StateFlow<Long>) {
    val timeRemaining by timeRemainingFlow.collectAsStateWithLifecycle()

    val seconds = (timeRemaining / 1000) % 60
    val minutes = (timeRemaining / (1000 * 60)) % 60
    val hours = (timeRemaining / (1000 * 60 * 60)) % 24
    val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    Text(
        text = timeString,
        color = Color.White,
        fontSize = 52.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 2.sp
    )
}

@Composable
fun SpaceBackground(
    currentDistortionIndex: Int = 0,
    nextDistortionIndex: Int = 1,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 1. Static Traveler Background Image
        val painter = androidx.compose.ui.res.painterResource(id = com.example.distortiontracker.R.drawable.traveler_background)
        androidx.compose.foundation.Image(
            painter = painter,
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // 2. Animated Space Overlay (Twinkling stars + planets)
        AnimatedSpaceOverlay(
            currentDistortionIndex = currentDistortionIndex,
            nextDistortionIndex = nextDistortionIndex
        )
    }
}

@Composable
fun AnimatedSpaceOverlay(
    currentDistortionIndex: Int,
    nextDistortionIndex: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "planetAnimation")
    
    // Slow rotation for planets
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 80000, easing = LinearEasing)
        ),
        label = "planetRotation"
    )

    // Twinkling stars phase
    val starPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing)
        ),
        label = "starPhase"
    )

    // Cache star coordinates and sizes to prevent allocations and random calculations inside the draw loop
    val stars = remember {
        val random = java.util.Random(100)
        List(61) {
            Triple(random.nextFloat(), random.nextFloat(), random.nextFloat() * 4f + 1f)
        }
    }

    val currentPlanetRes = getPlanetCurrentDrawableRes(currentDistortionIndex)
    val nextPlanetRes = getPlanetNextDrawableRes(nextDistortionIndex)

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Twinkling Stars Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            stars.forEachIndexed { i, star ->
                val x = star.first * width
                val y = star.second * height
                val starSize = star.third
                val twinkle = kotlin.math.sin(starPhase + i) * 0.4f + 0.6f
                drawCircle(
                    color = Color.White.copy(alpha = twinkle),
                    radius = starSize,
                    center = Offset(x, y)
                )
            }
        }

        // 2. Current Distorted Planet (large, back-right)
        val currentPainter = androidx.compose.ui.res.painterResource(id = currentPlanetRes)
        androidx.compose.foundation.Image(
            painter = currentPainter,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 10.dp, y = 200.dp)
                .size(150.dp)
                .graphicsLayer {
                    rotationZ = rotation
                }
        )

        // 3. Next Planet (smaller, overlapping in front on the bottom-right)
        val nextPainter = androidx.compose.ui.res.painterResource(id = nextPlanetRes)
        androidx.compose.foundation.Image(
            painter = nextPainter,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-30).dp, y = 260.dp)
                .size(80.dp)
                .graphicsLayer {
                    rotationZ = -rotation * 1.2f
                }
        )
    }
}

fun getPlanetCurrentDrawableRes(index: Int): Int {
    return when (index) {
        0 -> com.example.distortiontracker.R.drawable.planet_dreaming_city_current
        1 -> com.example.distortiontracker.R.drawable.planet_savathun_current
        2 -> com.example.distortiontracker.R.drawable.planet_moon_current
        3 -> com.example.distortiontracker.R.drawable.planet_europa_current
        4 -> com.example.distortiontracker.R.drawable.planet_nessus_current
        5 -> com.example.distortiontracker.R.drawable.planet_cosmodrome_current
        6 -> com.example.distortiontracker.R.drawable.planet_edz_current
        else -> com.example.distortiontracker.R.drawable.planet_dreaming_city_current
    }
}

fun getPlanetNextDrawableRes(index: Int): Int {
    return when (index) {
        0 -> com.example.distortiontracker.R.drawable.planet_dreaming_city_next
        1 -> com.example.distortiontracker.R.drawable.planet_savathun_next
        2 -> com.example.distortiontracker.R.drawable.planet_moon_next
        3 -> com.example.distortiontracker.R.drawable.planet_europa_next
        4 -> com.example.distortiontracker.R.drawable.planet_nessus_next
        5 -> com.example.distortiontracker.R.drawable.planet_cosmodrome_next
        6 -> com.example.distortiontracker.R.drawable.planet_edz_next
        else -> com.example.distortiontracker.R.drawable.planet_dreaming_city_next
    }
}

@Composable
fun androidx.compose.foundation.layout.BoxScope.GlassLayer(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape,
    color: Color = Color.Unspecified,
    brush: Brush? = null,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    borderColor: Color = GlassBorder,
    borderBrush: Brush? = null
) {
    val blurEffect = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.graphics.RenderEffect.createBlurEffect(
                40f, 40f, android.graphics.Shader.TileMode.CLAMP
            ).asComposeRenderEffect()
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .matchParentSize()
            .clip(shape)
            .graphicsLayer {
                if (blurEffect != null) {
                    renderEffect = blurEffect
                    clip = true
                }
            }
            .then(
                if (brush != null) Modifier.background(brush)
                else if (color != Color.Unspecified) Modifier.background(color)
                else Modifier
            )
            .then(
                if (borderBrush != null) Modifier.border(borderWidth, borderBrush, shape)
                else Modifier.border(borderWidth, borderColor, shape)
            )
    )
}

// Custom card shape that bulges out in the center to accommodate the glowing ring
class DestinationCardShape(private val circleRadiusDp: androidx.compose.ui.unit.Dp = 80.dp) : androidx.compose.ui.graphics.Shape {
    private val cachedPath = androidx.compose.ui.graphics.Path()

    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = cachedPath
        path.reset()
        val width = size.width
        val height = size.height
        val circleRadius = with(density) { circleRadiusDp.toPx() }
        val rectHeight = height * 0.72f
        val rectTop = (height - rectHeight) / 2f
        val rectBottom = rectTop + rectHeight
        val cornerRadius = with(density) { 16.dp.toPx() }
        val centerX = width / 2f
        val centerY = height / 2f

        path.moveTo(cornerRadius, rectTop)
        
        val bulgeStartOffset = circleRadius * 1.2f
        path.lineTo(centerX - bulgeStartOffset, rectTop)
        path.cubicTo(
            centerX - circleRadius * 0.6f, rectTop,
            centerX - circleRadius * 0.6f, centerY - circleRadius,
            centerX, centerY - circleRadius
        )
        path.cubicTo(
            centerX + circleRadius * 0.6f, centerY - circleRadius,
            centerX + circleRadius * 0.6f, rectTop,
            centerX + bulgeStartOffset, rectTop
        )
        
        path.lineTo(width - cornerRadius, rectTop)
        path.quadraticTo(width, rectTop, width, rectTop + cornerRadius)
        
        path.lineTo(width, rectBottom - cornerRadius)
        path.quadraticTo(width, rectBottom, width - cornerRadius, rectBottom)
        
        path.lineTo(centerX + bulgeStartOffset, rectBottom)
        path.cubicTo(
            centerX + circleRadius * 0.6f, rectBottom,
            centerX + circleRadius * 0.6f, centerY + circleRadius,
            centerX, centerY + circleRadius
        )
        path.cubicTo(
            centerX - circleRadius * 0.6f, centerY + circleRadius,
            centerX - circleRadius * 0.6f, rectBottom,
            centerX - bulgeStartOffset, rectBottom
        )
        
        path.lineTo(cornerRadius, rectBottom)
        path.quadraticTo(0f, rectBottom, 0f, rectBottom - cornerRadius)
        
        path.lineTo(0f, rectTop + cornerRadius)
        path.quadraticTo(0f, rectTop, cornerRadius, rectTop)
        
        path.close()
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}
