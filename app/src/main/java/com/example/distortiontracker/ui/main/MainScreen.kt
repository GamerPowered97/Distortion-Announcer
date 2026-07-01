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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
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
import androidx.compose.foundation.horizontalScroll
import java.util.Calendar
import java.util.Locale

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
            GlowingRing(size = 120f, destinationIndex = 2)
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
    var showInfoDialog by remember { mutableStateOf(false) }
    val theme = remember(state.currentDistortionIndex) { getDestinationTheme(state.currentDistortionIndex) }

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
                .height(180.dp)
                .clickable { showInfoDialog = true },
            contentAlignment = Alignment.Center
        ) {
            GlassLayer(
                shape = DestinationCardShape(80.dp),
                color = GlassBackground
            )
            
            // Glowing Ring in the center
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                GlowingRing(size = 140f, destinationIndex = state.currentDistortionIndex)
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
        // RIFT BORDER CONTAINER WRAPPING TIMER & UP NEXT CARDS
        // ═══════════════════════════════════════
        RiftBorderContainer(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TIMER CARD
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    GlassLayer(
                        shape = RoundedCornerShape(12.dp),
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                theme.primaryColor.copy(alpha = 0.33f),
                                GlassBackground,
                                theme.primaryColor.copy(alpha = 0.33f)
                            )
                        )
                    )
                    Box(modifier = Modifier.padding(vertical = 20.dp)) {
                        CountdownTimer(timeRemainingFlow = timeRemainingFlow)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // UP NEXT BAR
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassLayer(
                        shape = RoundedCornerShape(8.dp),
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                theme.primaryColor.copy(alpha = 0.25f),
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
        // SOLAR SCHEDULE TIMELINE
        // ═══════════════════════════════════════
        SolarScheduleSection(currentDistortionIndex = state.currentDistortionIndex)

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

    if (showInfoDialog) {
        val info = getDestinationInfo(state.currentDistortionIndex)
        val theme = getDestinationTheme(state.currentDistortionIndex)
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text(
                    text = info.title,
                    color = theme.primaryColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = info.lore,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(theme.accentColor.copy(alpha = 0.3f))
                    )
                    Column {
                        Text(
                            text = "KEY ACTIVITIES",
                            color = theme.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = info.activities,
                            color = Color(0xFFE0E0E0),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ZONE POINTERS",
                            color = theme.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = info.tips,
                            color = Color(0xFFE0E0E0),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("CLOSE", color = theme.primaryColor, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF15181F),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, theme.primaryColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        )
    }
}

// ═══════════════════════════════════════════════
// GLOWING RED RING — the signature distortion icon
// ═══════════════════════════════════════════════
data class SparkParticle(
    val baseAngle: Float,
    val radiusOffset: Float,
    val speedMultiplier: Float,
    val baseSize: Float
)

@Composable
fun GlowingRing(size: Float, destinationIndex: Int = 2) {
    val theme = remember(destinationIndex) { getDestinationTheme(destinationIndex) }
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

    // Breathing halo aura animation
    val breathingFactor by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingAura"
    )

    val densityLocal = LocalDensity.current
    
    // Cache stroke allocations to prevent object creation during draw execution
    val thickGlowStroke = remember(densityLocal) { Stroke(width = with(densityLocal) { 12.dp.toPx() }, join = StrokeJoin.Round) }
    val mediumGlowStroke = remember(densityLocal) { Stroke(width = with(densityLocal) { 5.dp.toPx() }, join = StrokeJoin.Round) }
    val coreStroke = remember(densityLocal) { Stroke(width = with(densityLocal) { 2.dp.toPx() }, join = StrokeJoin.Round) }
    val wispStroke1 = remember(densityLocal) { Stroke(width = with(densityLocal) { 2.dp.toPx() }, join = StrokeJoin.Round) }
    val wispStroke2 = remember(densityLocal) { Stroke(width = with(densityLocal) { 1.5.dp.toPx() }, join = StrokeJoin.Round) }

    // Cache gradient color lists
    val darkVoidColor = remember(theme) {
        Color(
            red = theme.primaryColor.red * 0.12f,
            green = theme.primaryColor.green * 0.12f,
            blue = theme.primaryColor.blue * 0.12f,
            alpha = 1f
        )
    }
    val darkVoidColorMid = remember(theme) {
        Color(
            red = theme.primaryColor.red * 0.05f,
            green = theme.primaryColor.green * 0.05f,
            blue = theme.primaryColor.blue * 0.05f,
            alpha = 1f
        )
    }
    val riftColors = remember(darkVoidColor, darkVoidColorMid) {
        listOf(darkVoidColor, darkVoidColorMid, Color(0xFF010204))
    }
    val vortexColors = remember(theme) { listOf(theme.primaryColor.copy(alpha = 0.25f), Color.Transparent, theme.accentColor.copy(alpha = 0.25f)) }
    val coronaColors = remember(theme) { listOf(theme.primaryColor.copy(alpha = 0.45f), theme.primaryColor.copy(alpha = 0.15f), Color.Transparent) }
    val auraColors = remember(theme) {
        listOf(
            theme.primaryColor.copy(alpha = 0.25f),
            theme.accentColor.copy(alpha = 0.05f),
            Color.Transparent
        )
    }

    // Cache spark particles
    val sparks = remember {
        val random = java.util.Random(42)
        List(8) {
            SparkParticle(
                baseAngle = random.nextFloat() * 2f * Math.PI.toFloat(),
                radiusOffset = random.nextFloat() * 20f - 10f,
                speedMultiplier = 0.5f + random.nextFloat() * 1.5f,
                baseSize = 2f + random.nextFloat() * 3f
            )
        }
    }

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
        
        // Draw the rift interior: dark backing void
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

        // Breathing halo aura behind the main circle
        drawCircle(
            brush = Brush.radialGradient(
                colors = auraColors,
                center = center,
                radius = radius * 1.6f * breathingFactor
            ),
            radius = radius * 1.6f * breathingFactor,
            center = center
        )

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
            // Procedural noise using phase and extra higher-frequency noise terms (3rd and 4th harmonics)
            val noise = kotlin.math.sin(angle * 8f + phase * 2f) * 0.04f +
                        kotlin.math.cos(angle * 16f - phase) * 0.02f +
                        kotlin.math.sin(angle * 28f + phase * 3f) * 0.01f +
                        kotlin.math.cos(angle * 40f - phase * 4f) * 0.005f +
                        kotlin.math.sin(angle * 56f + phase * 5f) * 0.002f
            
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
        drawPath(path = mainPath, color = theme.primaryColor.copy(alpha = 0.35f), style = thickGlowStroke)
        drawPath(path = mainPath, color = theme.accentColor.copy(alpha = 0.75f), style = mediumGlowStroke)
        
        val coreColor = theme.primaryColor.copy(alpha = 0.2f).let {
            Color(
                red = it.red * it.alpha + (1f - it.alpha),
                green = it.green * it.alpha + (1f - it.alpha),
                blue = it.blue * it.alpha + (1f - it.alpha),
                alpha = 1f
            )
        }
        drawPath(path = mainPath, color = coreColor, style = coreStroke)

        // Draw orbiting spark particles
        sparks.forEach { spark ->
            val angle = spark.baseAngle + phase * spark.speedMultiplier
            val sparkRadius = currentRadius + spark.radiusOffset * density
            val x = center.x + sparkRadius * kotlin.math.cos(angle)
            val y = center.y + sparkRadius * kotlin.math.sin(angle)
            
            val alpha = (kotlin.math.sin(phase * 3f + spark.baseAngle) * 0.5f + 0.5f) * 0.8f
            drawCircle(
                color = theme.accentColor.copy(alpha = alpha),
                radius = spark.baseSize * density,
                center = Offset(x, y)
            )
        }

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
            drawPath(path = wispPath1, color = theme.primaryColor.copy(alpha = 0.6f), style = wispStroke1)

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
            drawPath(path = wispPath2, color = theme.accentColor.copy(alpha = 0.5f), style = wispStroke2)
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
fun RiftBorderContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "riftBorderAnimation")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing)
        ),
        label = "riftBorderPhase"
    )

    val density = LocalDensity.current
    val thickGlowStroke = remember(density) { Stroke(width = with(density) { 6.dp.toPx() }, join = StrokeJoin.Round) }
    val coreStroke = remember(density) { Stroke(width = with(density) { 2.dp.toPx() }, join = StrokeJoin.Round) }
    val borderPath = remember { androidx.compose.ui.graphics.Path() }

    Box(
        modifier = modifier.padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
        Canvas(modifier = Modifier.matchParentSize()) {
            val margin = 2.dp.toPx()
            val width = size.width
            val height = size.height

            val left = margin
            val right = width - margin
            val top = margin
            val bottom = height - margin

            borderPath.reset()
            val segments = 20
            var first = true

            // 1. Top Edge: left to right
            for (i in 0..segments) {
                val t = i.toFloat() / segments
                val x = left + t * (right - left)
                val noise = kotlin.math.sin(t * 30f + phase * 4f) * 3.dp.toPx() +
                            kotlin.math.cos(t * 70f - phase * 2f) * 1.5.dp.toPx()
                val y = top + noise
                if (first) {
                    borderPath.moveTo(x, y)
                    first = false
                } else {
                    borderPath.lineTo(x, y)
                }
            }

            // 2. Right Edge: top to bottom
            for (i in 1..segments) {
                val t = i.toFloat() / segments
                val y = top + t * (bottom - top)
                val noise = kotlin.math.sin(t * 30f - phase * 4f) * 3.dp.toPx() +
                            kotlin.math.cos(t * 70f + phase * 2f) * 1.5.dp.toPx()
                val x = right + noise
                borderPath.lineTo(x, y)
            }

            // 3. Bottom Edge: right to left
            for (i in 1..segments) {
                val t = i.toFloat() / segments
                val x = right - t * (right - left)
                val noise = kotlin.math.sin(t * 30f + phase * 4f) * 3.dp.toPx() +
                            kotlin.math.cos(t * 70f - phase * 2f) * 1.5.dp.toPx()
                val y = bottom + noise
                borderPath.lineTo(x, y)
            }

            // 4. Left Edge: bottom to top
            for (i in 1..segments) {
                val t = i.toFloat() / segments
                val y = bottom - t * (bottom - top)
                val noise = kotlin.math.sin(t * 30f - phase * 4f) * 3.dp.toPx() +
                            kotlin.math.cos(t * 70f + phase * 2f) * 1.5.dp.toPx()
                val x = left + noise
                borderPath.lineTo(x, y)
            }

            borderPath.close()

            // Draw thick, semi-transparent soft red glow stroke (width ~6.dp, color Color(0xFFE53935).copy(alpha = 0.35f))
            drawPath(
                path = borderPath,
                color = Color(0xFFE53935).copy(alpha = 0.35f),
                style = thickGlowStroke
            )
            // Draw thin, bright core stroke (width ~2.dp, color Color(0xFFFF8A80))
            drawPath(
                path = borderPath,
                color = Color(0xFFFF8A80),
                style = coreStroke
            )
        }
    }
}

@Composable
fun SpaceBackground(
    currentDistortionIndex: Int = 0,
    nextDistortionIndex: Int = 1,
    modifier: Modifier = Modifier
) {
    val theme = remember(currentDistortionIndex) { getDestinationTheme(currentDistortionIndex) }
    Box(modifier = modifier.fillMaxSize()) {
        // 1. Static Traveler Background Image (tinted via ColorFilter)
        val painter = androidx.compose.ui.res.painterResource(id = com.example.distortiontracker.R.drawable.traveler_background)
        androidx.compose.foundation.Image(
            painter = painter,
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            colorFilter = ColorFilter.tint(
                color = theme.primaryColor.copy(alpha = 0.5f),
                blendMode = BlendMode.Color
            ),
            modifier = Modifier.fillMaxSize()
        )
        
        // Radial gradient vignette overlay on top of the image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000))
                    )
                )
        )
        
        // Nebula gradient overlay on top of the background image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = theme.gradientColors
                    )
                )
        )
        
        // 2. Animated Space Overlay (Twinkling stars + planets)
        AnimatedSpaceOverlay(
            currentDistortionIndex = currentDistortionIndex,
            nextDistortionIndex = nextDistortionIndex,
            theme = theme
        )
    }
}

@Composable
fun AnimatedSpaceOverlay(
    currentDistortionIndex: Int,
    nextDistortionIndex: Int,
    theme: DestinationTheme
) {
    val infiniteTransition = rememberInfiniteTransition(label = "planetAnimation")
    
    // Clockwise slow rotation for the current large planet
    val currentRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 80000, easing = LinearEasing)
        ),
        label = "currentPlanetRotation"
    )

    // Counter-clockwise slow rotation for the next small planet
    val nextRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 66000, easing = LinearEasing)
        ),
        label = "nextPlanetRotation"
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
        // 1. Twinkling Stars Canvas (tinted with theme particles)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            stars.forEachIndexed { i, star ->
                val x = star.first * width
                val y = star.second * height
                val starSize = star.third
                val twinkle = kotlin.math.sin(starPhase + i) * 0.4f + 0.6f
                val color = if (i % 3 == 0) {
                    theme.particleColor.copy(alpha = twinkle)
                } else {
                    Color.White.copy(alpha = twinkle)
                }
                drawCircle(
                    color = color,
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
                    rotationZ = currentRotation
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
                    rotationZ = nextRotation
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

// ═══════════════════════════════════════════════
// THEME & LORE HELPER DATA CLASSES & METHODS
// ═══════════════════════════════════════════════

data class DestinationTheme(
    val primaryColor: Color,
    val accentColor: Color,
    val gradientColors: List<Color>,
    val particleColor: Color
)

fun getDestinationTheme(index: Int): DestinationTheme {
    return when (index) {
        0 -> DestinationTheme(
            primaryColor = Color(0xFFBA68C8), // Violet
            accentColor = Color(0xFF00E5FF),  // Teal
            gradientColors = listOf(Color(0x4D9C27B0), Color(0x1A00E5FF), Color(0xD9000000)),
            particleColor = Color(0xFF00E5FF)
        )
        1 -> DestinationTheme(
            primaryColor = Color(0xFF76FF03), // Hive Green
            accentColor = Color(0xFFEEFF41),  // Lime
            gradientColors = listOf(Color(0x4076FF03), Color(0x1AEEFF41), Color(0xD9000000)),
            particleColor = Color(0xFFEEFF41)
        )
        2 -> DestinationTheme(
            primaryColor = Color(0xFFE53935), // Crimson
            accentColor = Color(0xFFFF8A80),  // Light Scarlet
            gradientColors = listOf(Color(0x4DE53935), Color(0x26311B92), Color(0xD9000000)),
            particleColor = Color(0xFFFF8A80)
        )
        3 -> DestinationTheme(
            primaryColor = Color(0xFF80D8FF), // Icy Blue
            accentColor = Color(0xFFE0F7FA),  // White/Cyan
            gradientColors = listOf(Color(0x4D0D47A1), Color(0x1A006064), Color(0xD9000000)),
            particleColor = Color(0xFFE0F7FA)
        )
        4 -> DestinationTheme(
            primaryColor = Color(0xFFCD7F32), // Vex Copper
            accentColor = Color(0xFFFF1744),  // Red Flora
            gradientColors = listOf(Color(0x593E2723), Color(0x1AB71C1C), Color(0xD9000000)),
            particleColor = Color(0xFFFFD54F)
        )
        5 -> DestinationTheme(
            primaryColor = Color(0xFFD84315), // Rust Orange
            accentColor = Color(0xFF29B6F6),  // Sky Blue
            gradientColors = listOf(Color(0x594E342E), Color(0x1A01579B), Color(0xD9000000)),
            particleColor = Color(0xFF80D8FF)
        )
        6 -> DestinationTheme(
            primaryColor = Color(0xFF2E7D32), // Forest Green
            accentColor = Color(0xFF90A4AE),  // Slate Grey
            gradientColors = listOf(Color(0x4D1B5E20), Color(0x2637474F), Color(0xD9000000)),
            particleColor = Color(0xFFA5D6A7)
        )
        else -> DestinationTheme(
            primaryColor = Color(0xFFE53935),
            accentColor = Color(0xFFFF8A80),
            gradientColors = listOf(Color(0x4DE53935), Color(0x26311B92), Color(0xD9000000)),
            particleColor = Color(0xFFFF8A80)
        )
    }
}

data class DestinationInfo(
    val title: String,
    val lore: String,
    val activities: String,
    val tips: String
)

fun getDestinationInfo(index: Int): DestinationInfo {
    return when (index) {
        0 -> DestinationInfo(
            title = "DREAMING CITY",
            lore = "A mystical realm hidden in the Reef, built by the Awoken as a convergence of Light and Dark. Beware the three-week curse cycle.",
            activities = "Blind Well, Shattered Throne, Ascendant Challenges",
            tips = "Seek out the cat statues with small gifts, and watch the Ascendant plane for hidden platforms."
        )
        1 -> DestinationInfo(
            title = "SAVATHÛN'S THRONE WORLD",
            lore = "A shimmering court of Light, malice, and illusion. The Witch Queen's mind made manifest, where truth is a funny thing.",
            activities = "Wellspring, Altars of Reflection, Lucent Hive Patrols",
            tips = "Use Deepsight to reveal hidden pathways and chest locations throughout the fluorescent swamps."
        )
        2 -> DestinationInfo(
            title = "MOON",
            lore = "The scarred surface of Earth's companion. Beneath the dust lies the Scarlet Keep, home to Hive Nightmares and a silent Pyramid ship.",
            activities = "Altars of Sorrow, Pit of Heresy, Nightmare Hunts",
            tips = "Defeat Nightmares to earn Phantasmal Fragments, and clear Altars of Sorrow for unique weapons."
        )
        3 -> DestinationInfo(
            title = "EUROPA",
            lore = "A frozen wasteland of ice and secrets. Beneath the glacial crust lies the Braytech Exoscience facility and the Deep Stone Crypt.",
            activities = "Empire Hunts, Exo Challenges, Brig Patrols",
            tips = "Watch out for freezing blizzards that reduce visibility. Use Stasis to shatter frozen targets."
        )
        4 -> DestinationInfo(
            title = "NESSUS",
            lore = "An unstable Centaur planetoid, converted into a machine world by the Vex. Red vegetation clings to copper structures.",
            activities = "Battlegrounds, Vex Spire Integrations, Failsafe's Bounties",
            tips = "Climb the giant red-leafed trees for sniper vantage points. Watch for milk waterfalls—Vex radiolarian fluid is highly toxic."
        )
        5 -> DestinationInfo(
            title = "COSMODROME",
            lore = "A rusted graveyard of humanity's golden age. Old colony ships stand as silent monuments, surrounded by scavengers.",
            activities = "Devils' Lair, Shaw Han's Patrols, Lost Sector farms",
            tips = "Great zone for new Guardians to test weapons and farm Spinmetal Leaves. Keep an eye out for Fallen Walkers."
        )
        6 -> DestinationInfo(
            title = "EDZ",
            lore = "The European Dead Zone. Nature has reclaimed the ruins of a bustling human territory. Cabal warbases clash with Fallen scavengers.",
            activities = "Arms Dealer, Glimmer Extractions, Cabal Excavations",
            tips = "Explore the ruins of Trostland for hidden cellars. Watch out for high-priority targets roaming the forest outskirts."
        )
        else -> DestinationInfo(
            title = "UNKNOWN DESTINATION",
            lore = "A zone lost to the archives. Proceed with caution.",
            activities = "N/A",
            tips = "Keep your weapon loaded."
        )
    }
}

fun getDestinationShortName(name: String): String {
    return when (name) {
        "SAVATHUN'S THRONE WORLD" -> "THRONE WORLD"
        "SAVATHÛN'S THRONE WORLD" -> "THRONE WORLD"
        "DREAMING CITY" -> "DREAMING CITY"
        "COSMODROME" -> "COSMODROME"
        else -> name
    }
}

@Composable
fun SolarScheduleSection(
    currentDistortionIndex: Int
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
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
                    text = "SOLAR SCHEDULE (24H)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = "Forecast of upcoming hourly paracausal distortions.",
                        color = Color(0xFFB0BEC5),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                    )

                    // Horizontally scrollable row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val calendar = Calendar.getInstance()
                        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

                        for (h in 0..23) {
                            val targetHour = (currentHour + h) % 24
                            val timeLabel = if (h == 0) "NOW" else String.format(Locale.US, "%02d:00", targetHour)
                            val destIndex = (currentDistortionIndex + h) % DistortionManager.DESTINATIONS.size
                            val destName = DistortionManager.DESTINATIONS[destIndex]
                            val destTheme = getDestinationTheme(destIndex)

                            // Small item card
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(90.dp)
                            ) {
                                GlassLayer(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x700E1116),
                                    borderColor = destTheme.primaryColor.copy(alpha = 0.4f)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = timeLabel,
                                        color = if (h == 0) NeonCrimson else Color(0xFFB0BEC5),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = getDestinationShortName(destName),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        lineHeight = 13.sp
                                    )
                                    // A tiny accent line at the bottom
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .height(2.dp)
                                            .background(destTheme.primaryColor, RoundedCornerShape(1.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
