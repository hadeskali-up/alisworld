package com.alisworld.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AlisPurple,
    onPrimary = Surface,
    primaryContainer = AlisLavenderDark,
    onPrimaryContainer = Surface,
    secondary = DarkMuted,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = DarkBorder,
    error = RedLoss,
    errorContainer = DarkRedSoft,
    onErrorContainer = DarkText
)

private val LightColorScheme = lightColorScheme(
    primary = AlisPurple,
    onPrimary = Surface,
    primaryContainer = AlisLavender,
    onPrimaryContainer = AlisPurpleDark,
    secondary = Slate,
    background = Mist,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Slate,
    outline = Border,
    error = RedLoss,
    errorContainer = RedSoft,
    onErrorContainer = RedLoss
)

@Composable
fun AlisWorldTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
