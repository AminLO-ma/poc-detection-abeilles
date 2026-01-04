package com.example.beedetectionapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Thème Sombre
private val DarkColorScheme = darkColorScheme(
    primary = HoneyGold,
    onPrimary = HiveBlack,
    secondary = HoneyOrange,
    background = DeepCharcoal,
    surface = HiveBlack,
    onBackground = PollenWhite,
    onSurface = PollenWhite
)

// Thème Clair
private val LightColorScheme = lightColorScheme(
    primary = HoneyOrange,       // Lisibilité sur clair
    onPrimary = PollenWhite,
    secondary = HoneyGold,
    background = PollenWhite,
    surface = WaxBeige,
    onBackground = HiveBlack,
    onSurface = HiveBlack
)

@Composable
fun HoneyBeeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // 1. Contrôleur fenêtre
            val insetsController = WindowCompat.getInsetsController(window, view)

            // 2. Gestion icônes status bar
            // Sombre -> Icônes claires | Clair -> Icônes foncées
            // Si le thème est clair (!darkTheme est vrai) -> Icônes foncées (isAppearanceLight = true)
            insetsController.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
