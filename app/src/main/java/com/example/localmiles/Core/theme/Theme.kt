package com.example.localmiles.Core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

// Top app bars/headers use primaryContainer (see AppTopBar), CTAs use primary —
// this matches the brand mockups: dark teal chrome, green action buttons.
private val LightColorScheme = lightColorScheme(
    primary = LocalMilesGreen,
    onPrimary = Color.White,
    primaryContainer = LocalMilesDarkTeal,
    onPrimaryContainer = Color.White,

    secondary = LocalMilesLime,
    onSecondary = LocalMilesDarkTeal,
    secondaryContainer = Color(0xFFE8F5DC),
    onSecondaryContainer = LocalMilesDarkTeal,

    tertiary = LocalMilesOrange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCE8D0),
    onTertiaryContainer = Color(0xFF7A3B00),

    background = LocalMilesBackground,
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = LocalMilesSurfaceVariant,
    onSurfaceVariant = LocalMilesGrey,

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

// Dark mode keeps the same brand hues for secondary/tertiary accents (they
// read fine on dark surfaces); primary/container are adjusted for contrast.
private val DarkColorScheme = darkColorScheme(
    primary = LocalMilesLime,
    onPrimary = Color(0xFF10210A),
    primaryContainer = LocalMilesDarkTeal,
    onPrimaryContainer = Color.White,

    secondary = LocalMilesLime,
    onSecondary = Color(0xFF10210A),
    secondaryContainer = Color(0xFF2E4A1B),
    onSecondaryContainer = Color(0xFFDCF5C4),

    tertiary = LocalMilesOrange,
    onTertiary = Color(0xFF3A1E00),
    tertiaryContainer = Color(0xFF5A3000),
    onTertiaryContainer = Color(0xFFFCE8D0),

    background = LocalMilesDarkBackground,
    onBackground = Color(0xFFE2E2E5),
    surface = LocalMilesDarkSurface,
    onSurface = Color(0xFFE2E2E5),
    surfaceVariant = LocalMilesDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB6C0C3),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

// Dynamic color (Material You) is intentionally not offered here — it would
// override the brand palette with the device wallpaper's colors on
// Android 12+, so the app always renders in LocalMiles' own colors instead.
@Composable
fun LocalMilesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
