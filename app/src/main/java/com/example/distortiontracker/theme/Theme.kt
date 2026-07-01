package com.example.distortiontracker.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DestinyColorScheme = darkColorScheme(
    primary = NeonCrimson,
    secondary = TextAccent,
    tertiary = NeonCrimson,
    background = VoidSpaceDark,
    surface = VoidSpaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun DistortionTrackerTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
      colorScheme = DestinyColorScheme,
      typography = Typography,
      content = content
  )
}
