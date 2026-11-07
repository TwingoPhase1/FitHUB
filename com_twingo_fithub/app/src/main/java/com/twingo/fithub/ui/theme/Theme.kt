package com.twingo.fithub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FitHubColorScheme = darkColorScheme(
    primary = FitHubLime,
    onPrimary = Color(0xFF102007),
    primaryContainer = FitHubLimeDeep,
    onPrimaryContainer = FitHubLime,
    secondary = FitHubOrange,
    onSecondary = Color(0xFF2A1500),
    tertiary = FitHubBlue,
    onTertiary = Color(0xFF001E2B),
    background = FitHubBackground,
    onBackground = FitHubText,
    surface = FitHubSurface,
    onSurface = FitHubText,
    surfaceVariant = FitHubSurfaceElevated,
    onSurfaceVariant = FitHubTextMuted,
    outline = FitHubOutline,
    error = FitHubError,
)

/**
 * Thème sombre unique, sans couleur dynamique : le kiosk doit conserver
 * un contraste stable et lisible pendant l'effort.
 */
@Composable
fun FitHUBTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FitHubColorScheme,
        typography = Typography,
        content = content,
    )
}
