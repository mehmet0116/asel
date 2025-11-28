package com.aikodasistani.aikodasistani.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Primary colors
private val Primary = Color(0xFF2196F3)
private val PrimaryDark = Color(0xFF1976D2)
private val PrimaryLight = Color(0xFFBBDEFB)
private val Secondary = Color(0xFF4CAF50)
private val SecondaryDark = Color(0xFF388E3C)
private val Tertiary = Color(0xFFFF9800)

// Light color scheme
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    primaryContainer = PrimaryLight,
    secondary = Secondary,
    secondaryContainer = Color(0xFFC8E6C9),
    tertiary = Tertiary,
    tertiaryContainer = Color(0xFFFFE0B2),
    surface = Color(0xFFFFFBFE),
    surfaceVariant = Color(0xFFE7E0EC),
    background = Color(0xFFFFFBFE),
    error = Color(0xFFB00020),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),
    onBackground = Color(0xFF1C1B1F),
    onError = Color.White,
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

// Dark color scheme
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    primaryContainer = PrimaryDark,
    secondary = Color(0xFFA5D6A7),
    secondaryContainer = SecondaryDark,
    tertiary = Color(0xFFFFCC80),
    tertiaryContainer = Color(0xFFE65100),
    surface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFF49454F),
    background = Color(0xFF1C1B1F),
    error = Color(0xFFCF6679),
    onPrimary = Color(0xFF003258),
    onSecondary = Color(0xFF003300),
    onTertiary = Color(0xFF4A2800),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    onBackground = Color(0xFFE6E1E5),
    onError = Color.Black,
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

/**
 * AI Kod Asistanı application theme for Jetpack Compose.
 * Supports light/dark modes with dynamic colors on Android 12+.
 */
@Composable
fun AIKodAsistaniTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
