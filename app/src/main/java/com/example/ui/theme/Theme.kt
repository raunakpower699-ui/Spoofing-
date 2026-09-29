package com.example.ui.theme

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

private val LiquidGlassColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF001F28),
    primaryContainer = Color(0xFF004E5F),
    onPrimaryContainer = Color(0xFFBCEBFF),
    secondary = NeonPurple,
    onSecondary = Color(0xFF2E004E),
    secondaryContainer = Color(0xFF4A148C),
    onSecondaryContainer = Color(0xFFE8B4FF),
    tertiary = NeonMagenta,
    onTertiary = Color(0xFF49001B),
    background = VoidBlack,
    onBackground = Color(0xFFE6EDF5),
    surface = GlassNavyDark,
    onSurface = Color(0xFFE6EDF5),
    surfaceVariant = GlassDeepBlue,
    onSurfaceVariant = Color(0xFFBAC7D5),
    outline = GlassBorderTint,
    error = NeonCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek dark glass aesthetic
    dynamicColor: Boolean = false, // Keep signature liquid neon aesthetic
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> LiquidGlassColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
