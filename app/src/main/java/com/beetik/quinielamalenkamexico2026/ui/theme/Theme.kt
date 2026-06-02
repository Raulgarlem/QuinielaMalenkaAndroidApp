package com.beetik.quinielamalenkamexico2026.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    secondary = GoldVariant,
    tertiary = Success,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = OnDarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkCard,
    onSurfaceVariant = OnDarkSurfaceVariant,
    outline = Gold.copy(alpha = 0.3f),
    outlineVariant = Color.Gray.copy(alpha = 0.3f)
)

@Composable
fun QuinielaMalenkaMexico2026Theme(
    darkTheme: Boolean = true, // Force dark theme for the style
    dynamicColor: Boolean = false, // Disable dynamic color to keep the brand style
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
