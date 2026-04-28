package com.balarmi.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    primaryContainer = GreenContainer,
    onPrimaryContainer = GreenOnContainer,
    error = RedAlarm,
    errorContainer = RedAlarmContainer,
    onErrorContainer = RedAlarmOnContainer,
)

private val DarkColors = darkColorScheme(
    primary = GreenContainer,
    onPrimary = GreenOnContainer,
    primaryContainer = GreenPrimary,
    onPrimaryContainer = GreenContainer,
    error = RedAlarm,
)

@Composable
fun BalarmiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
