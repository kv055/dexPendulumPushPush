package de.dexarb.pushpushrelay.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF1A1612)
val BrandGreen = Color(0xFF3FB950)
val BrandBlue = Color(0xFF58A6FF)
val BrandRed = Color(0xFFF85149)
val BrandPeach = Color(0xFFFF6B35)
val BrandAccent = Color(0xFFF7931A)

private val DarkScheme = darkColorScheme(
    primary = BrandBlue,
    onPrimary = Color(0xFF0D1117),
    secondary = BrandGreen,
    onSecondary = Color(0xFF0D1117),
    tertiary = BrandPeach,
    error = BrandRed,
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF21262D),
    onSurfaceVariant = Color(0xFF8B949E),
    outline = Color(0xFF30363D),
)

private val LightScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    secondary = BrandGreen,
    onSecondary = Color.White,
    tertiary = BrandPeach,
    error = BrandRed,
    background = Color(0xFFF6F8FB),
    onBackground = Color(0xFF1F2937),
    surface = Color.White,
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFE4E9F0),
    onSurfaceVariant = Color(0xFF667085),
    outline = Color(0xFFD0D7DE),
)

@Composable
fun PushRelayTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme
    MaterialTheme(colorScheme = scheme, content = content)
}
