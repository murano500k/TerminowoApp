package com.stc.terminowo.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF545F71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E3F8),
    onSecondaryContainer = Color(0xFF111C2B),
    tertiary = Color(0xFFE65100),
    onTertiary = Color.White,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    background = Color(0xFFFAFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFAFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF263141),
    secondaryContainer = Color(0xFF3C4858),
    onSecondaryContainer = Color(0xFFD7E3F8),
    tertiary = Color(0xFFFFB68C),
    onTertiary = Color(0xFF532200),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF)
)

@Composable
fun TerminowoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
