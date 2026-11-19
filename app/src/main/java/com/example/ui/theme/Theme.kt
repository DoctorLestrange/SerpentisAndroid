package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val SerpentisDarkColorScheme = darkColorScheme(
    primary = SerpentEmerald,
    onPrimary = ObsidianDeep,
    primaryContainer = SerpentEmeraldContainer,
    onPrimaryContainer = SerpentEmerald,
    secondary = CyberCyan,
    onSecondary = ObsidianDeep,
    secondaryContainer = CyberCyanContainer,
    onSecondaryContainer = CyberCyan,
    tertiary = GoldenOrb,
    onTertiary = ObsidianDeep,
    tertiaryContainer = Color(0xFF423005),
    onTertiaryContainer = GoldenOrb,
    error = CrimsonOrb,
    onError = Color.White,
    errorContainer = Color(0xFF4D0B22),
    onErrorContainer = Color(0xFFFFB2C5),
    background = ObsidianDeep,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = ObsidianBorder,
    outlineVariant = ObsidianElevated
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SerpentisTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = SerpentisDarkColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
