package com.balarmi.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GruvboxDark = darkColorScheme(
    primary = GruvboxYellow,
    onPrimary = GruvboxBg0,
    primaryContainer = GruvboxBg3,
    onPrimaryContainer = GruvboxYellow,
    secondary = GruvboxOrange,
    onSecondary = GruvboxBg0,
    tertiary = GruvboxGreen,
    onTertiary = GruvboxBg0,
    background = GruvboxBg0,
    onBackground = GruvboxFg1,
    surface = GruvboxBg1,
    onSurface = GruvboxFg1,
    surfaceVariant = GruvboxBg3,
    onSurfaceVariant = GruvboxGrey2,
    outline = GruvboxGrey1,
    outlineVariant = GruvboxBg5,
    error = GruvboxRed,
    onError = GruvboxBg0,
    errorContainer = GruvboxErrorContainer,
    onErrorContainer = GruvboxFg0,
)

@Composable
fun BalarmiTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(colorScheme = GruvboxDark, content = content)
}
