package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TealAccent,
    onPrimary = DarkTealOnAccent,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkCardBg,
    onSurface = TextPrimary,
    surfaceVariant = DarkBorder,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = SignalRed,
    onError = Color.White
)

@Composable
fun MicoderStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
