package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// --- Premium Genre-Adaptive Dynamic Themes (Material You Wallpaper Fallbacks) ---

private val ElectronicColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFF03DAC6),
    tertiary = Color(0xFFCF6679),
    background = Color(0xFF0C0714),
    surface = Color(0xFF140D22),
    onPrimary = Color(0xFF24005A),
    onBackground = Color(0xFFE6DFFF),
    onSurface = Color(0xFFE6DFFF)
)

private val AcousticColorScheme = darkColorScheme(
    primary = Color(0xFFFFB300),
    secondary = Color(0xFFE08B4E),
    tertiary = Color(0xFF8D6E63),
    background = Color(0xFF1F140C),
    surface = Color(0xFF2B1D12),
    onPrimary = Color(0xFF421E00),
    onBackground = Color(0xFFFBEBE0),
    onSurface = Color(0xFFFBEBE0)
)

private val IndieColorScheme = darkColorScheme(
    primary = Color(0xFFF06292),
    secondary = Color(0xFFBA68C8),
    tertiary = Color(0xFFFF8A80),
    background = Color(0xFF160E12),
    surface = Color(0xFF22161B),
    onPrimary = Color(0xFF560027),
    onBackground = Color(0xFFFAECF0),
    onSurface = Color(0xFFFAECF0)
)

private val CinematicColorScheme = darkColorScheme(
    primary = Color(0xFF4DD0E1),
    secondary = Color(0xFF81C784),
    tertiary = Color(0xFFFFD54F),
    background = Color(0xFF061017),
    surface = Color(0xFF0F1E2A),
    onPrimary = Color(0xFF00363A),
    onBackground = Color(0xFFE0F7FA),
    onSurface = Color(0xFFE0F7FA)
)

private val LofiColorScheme = darkColorScheme(
    primary = Color(0xFFB39DDB),
    secondary = Color(0xFFFFD54F),
    tertiary = Color(0xFFFFB74D),
    background = Color(0xFF110E18),
    surface = Color(0xFF1A1625),
    onPrimary = Color(0xFF311B92),
    onBackground = Color(0xFFEDE7F6),
    onSurface = Color(0xFFEDE7F6)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    themeGenre: String? = null, // When active, enables live album art adapting
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        // 1. Check if we have an active genre to dynamically adapt the app theme
        themeGenre != null -> {
            when (themeGenre.lowercase()) {
                "electronic" -> ElectronicColorScheme
                "acoustic" -> AcousticColorScheme
                "indie pop", "indie" -> IndieColorScheme
                "cinematic" -> CinematicColorScheme
                "lofi", "lo-fi" -> LofiColorScheme
                else -> ElectronicColorScheme
            }
        }
        // 2. Otherwise fall back to Android 12+ Wallpaper adaptation
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // 3. Fall back to standard statically polished material schemas
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
