package com.stc.terminowo.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** Accent red used for primary action buttons (scan, save, toggles). */
val AccentRed = Color(0xFFE8274C)
val AccentRedDark = Color(0xFFFF6B81)

/** Extra colors not covered by Material3 color scheme. */
data class ExtendedColors(
    val accentRed: Color = AccentRed
)

val LocalExtendedColors = compositionLocalOf { ExtendedColors() }

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFF4842A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC9),
    onPrimaryContainer = Color(0xFF331100),
    secondary = Color(0xFF755846),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBC9),
    onSecondaryContainer = Color(0xFF2B1709),
    tertiary = Color(0xFF1565C0),
    onTertiary = Color.White,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF201A17),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF201A17),
    surfaceVariant = Color(0xFFF4DED3),
    onSurfaceVariant = Color(0xFF52443B),
    surfaceContainerLow = Color(0xFFF5F5F5),
    surfaceContainer = Color(0xFFFFFFFF)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB690),
    onPrimary = Color(0xFF522100),
    primaryContainer = Color(0xFF743300),
    onPrimaryContainer = Color(0xFFFFDBC9),
    secondary = Color(0xFFE5BFA9),
    onSecondary = Color(0xFF422B1C),
    secondaryContainer = Color(0xFF5B4130),
    onSecondaryContainer = Color(0xFFFFDBC9),
    tertiary = Color(0xFF9ECAFF),
    onTertiary = Color(0xFF003258),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = Color(0xFF121212),
    onBackground = Color(0xFFECE0DA),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFECE0DA),
    surfaceVariant = Color(0xFF52443B),
    onSurfaceVariant = Color(0xFFD7C3B7),
    surfaceContainerLow = Color(0xFF121212),
    surfaceContainer = Color(0xFF1E1E1E)
)

@Composable
fun TerminowoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) ExtendedColors(AccentRedDark) else ExtendedColors(AccentRed)

    androidx.compose.runtime.CompositionLocalProvider(
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
