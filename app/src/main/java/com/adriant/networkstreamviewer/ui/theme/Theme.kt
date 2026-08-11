package com.adriant.networkstreamviewer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.adriant.networkstreamviewer.domain.model.AppTheme

private val DarkColorScheme =
    darkColorScheme(
        primary = NdiDarkCyan,
        onPrimary = Color(0xFF003640),
        secondary = Color(0xFF9ED9E5),
        onSecondary = Color(0xFF00363D),
        background = NdiDarkBackground,
        onBackground = Color(0xFFF4F5FA),
        surface = NdiDarkSurface,
        onSurface = Color(0xFFF4F5FA),
        surfaceVariant = NdiDarkSource,
        onSurfaceVariant = Color(0xFF9A9AA9),
        outline = Color(0xFF686879),
        outlineVariant = NdiDarkBorder,
        surfaceContainerLowest = Color(0xFF06060D),
        surfaceContainerLow = NdiDarkSurface,
        surfaceContainer = Color(0xFF11111C),
        surfaceContainerHigh = Color(0xFF171725),
        surfaceContainerHighest = Color(0xFF1D1D2B),
    )

private val LightColorScheme =
    lightColorScheme(
        primary = NdiLightCyan,
        onPrimary = Color.White,
        secondary = Color(0xFF3B6870),
        onSecondary = Color.White,
        background = NdiLightBackground,
        onBackground = Color(0xFF171A20),
        surface = NdiLightSurface,
        onSurface = Color(0xFF171A20),
        surfaceVariant = NdiLightSource,
        onSurfaceVariant = Color(0xFF5E6570),
        outline = Color(0xFF777E89),
        outlineVariant = NdiLightBorder,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF2F4F7),
        surfaceContainer = Color(0xFFEDF0F4),
        surfaceContainerHigh = Color(0xFFE7EAF0),
        surfaceContainerHighest = Color(0xFFE1E5EB),
    )

private val AmoledColorScheme =
    DarkColorScheme.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = Color(0xFF0B0B14),
        surfaceDim = Color.Black,
        surfaceBright = Color(0xFF161622),
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF05050A),
        surfaceContainer = Color(0xFF090910),
        surfaceContainerHigh = Color(0xFF10101B),
        surfaceContainerHighest = Color(0xFF161622),
    )

@Composable
fun NetworkStreamViewerTheme(
    appTheme: AppTheme = AppTheme.AMOLED,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (appTheme) {
            AppTheme.SYSTEM -> isSystemInDarkTheme()
            AppTheme.LIGHT -> false
            AppTheme.DARK, AppTheme.AMOLED -> true
        }
    val colorScheme =
        when {
            appTheme == AppTheme.AMOLED -> AmoledColorScheme
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
