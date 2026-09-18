package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VesperaDarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = TextPrimary,
    primaryContainer = CardSurface,
    onPrimaryContainer = SoftLavender,
    secondary = SoftLavender,
    onSecondary = DeepMidnight,
    secondaryContainer = BorderViolet,
    onSecondaryContainer = TextPrimary,
    tertiary = GlowingCyan,
    onTertiary = DeepMidnight,
    background = DeepMidnight,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    outline = BorderViolet
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = VesperaDarkColorScheme,
        typography = Typography,
        content = content
    )
}

