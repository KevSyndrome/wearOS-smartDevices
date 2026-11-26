package com.example.fitpulse.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * Wear Material 3 color scheme aligned with the FitPulse phone palette.
 * The watch is mostly black to save power, with a mint/green accent for the
 * primary action and the active workout state.
 */
private val FitPulseWearColors = ColorScheme(
    primary = Color(0xFF43DE9B),
    primaryDim = Color(0xFF00875A),
    primaryContainer = Color(0xFF00543A),
    onPrimary = Color(0xFF002114),
    onPrimaryContainer = Color(0xFFB9F5D8),
    secondary = Color(0xFF43DE9B),
    onSecondary = Color(0xFF002114),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE6F2EA),
    surfaceContainerLow = Color(0xFF0B0F0C),
    surfaceContainer = Color(0xFF101512),
    surfaceContainerHigh = Color(0xFF1A211C),
    onSurface = Color(0xFFE6F2EA),
    onSurfaceVariant = Color(0xFFA6B8AC),
    outline = Color(0xFF7C8F82),
    outlineVariant = Color(0xFF3A4A3F),
)

@Composable
fun FitPulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = FitPulseWearColors, content = content)
}
