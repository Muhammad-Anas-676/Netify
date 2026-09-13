package com.netify.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tokens matched to the approved UI mockup (cyan/blue accent, near-black dark surface).
val AccentCyan = Color(0xFF19E7D6)
val AccentBlue = Color(0xFF0AA3FF)
val GoodGreen = Color(0xFF3ECF8E)
val WarnAmber = Color(0xFFF2A93B)
val DangerRed = Color(0xFFFF6B6B)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0BB8AA),
    secondary = AccentBlue,
    background = Color(0xFFF3F6F7),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0E1519),
    onSurface = Color(0xFF0E1519),
    outline = Color(0xFFDCE3E5)
)

private val DarkColors = darkColorScheme(
    primary = AccentCyan,
    secondary = AccentBlue,
    background = Color(0xFF060A0F),
    surface = Color(0xFF0E141B),
    onBackground = Color(0xFFE9F1F4),
    onSurface = Color(0xFFE9F1F4),
    outline = Color(0xFF212A33)
)

@Composable
fun NetifyTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = NetifyTypography, content = content)
}
