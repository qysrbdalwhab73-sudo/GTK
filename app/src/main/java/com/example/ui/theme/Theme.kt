package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ClaudeAmberLight,
    onPrimary = ClaudeDarkBackground,
    primaryContainer = ClaudeAmberDark,
    onPrimaryContainer = ClaudeTextPrimary,
    secondary = ClaudeAmberPrimary,
    onSecondary = ClaudeDarkBackground,
    secondaryContainer = ClaudeDarkSurfaceVariant,
    onSecondaryContainer = ClaudeTextPrimary,
    tertiary = ClaudeTerracotta,
    onTertiary = ClaudeDarkBackground,
    background = ClaudeDarkBackground,
    onBackground = ClaudeTextPrimary,
    surface = ClaudeDarkSurface,
    onSurface = ClaudeTextPrimary,
    surfaceVariant = ClaudeDarkSurfaceVariant,
    onSurfaceVariant = ClaudeTextSecondary,
    outline = ClaudeDarkBorder,
    outlineVariant = ClaudeDarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = ClaudeAmberPrimary,
    onPrimary = ClaudeLightSurface,
    primaryContainer = ClaudeLightSurfaceVariant,
    onPrimaryContainer = ClaudeLightTextPrimary,
    secondary = ClaudeTerracotta,
    onSecondary = ClaudeLightSurface,
    secondaryContainer = ClaudeLightSurfaceVariant,
    onSecondaryContainer = ClaudeLightTextPrimary,
    tertiary = ClaudeAmberDark,
    onTertiary = ClaudeLightSurface,
    background = ClaudeLightBackground,
    onBackground = ClaudeLightTextPrimary,
    surface = ClaudeLightSurface,
    onSurface = ClaudeLightTextPrimary,
    surfaceVariant = ClaudeLightSurfaceVariant,
    onSurfaceVariant = ClaudeLightTextSecondary,
    outline = ClaudeLightBorder,
    outlineVariant = ClaudeLightSurfaceVariant
)

@Composable
fun GameReverseTheme(
    darkTheme: Boolean = true, // Default to Claude Dark luxury theme
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
