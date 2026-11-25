package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.model.AppAccent
import com.example.model.AppThemeMode

fun buildSerpentisColorScheme(
    themeMode: AppThemeMode,
    accent: AppAccent,
    systemInDarkTheme: Boolean
): Pair<ColorScheme, Boolean> {
    val effectiveIsDark = when (themeMode) {
        AppThemeMode.DARK_GRAPHITE, AppThemeMode.MIDNIGHT_CYBER -> true
        AppThemeMode.SOFT_LIGHT -> false
        AppThemeMode.SYSTEM -> systemInDarkTheme
    }

    val scheme = when {
        !effectiveIsDark -> {
            // Warm Off-White Light Scheme (never pure #FFFFFF glare)
            lightColorScheme(
                primary = accent.lightPrimary,
                onPrimary = accent.lightOnPrimary,
                primaryContainer = accent.lightPrimaryContainer,
                onPrimaryContainer = accent.lightOnPrimaryContainer,
                secondary = accent.lightSecondary,
                onSecondary = accent.lightOnSecondary,
                secondaryContainer = accent.lightSecondaryContainer,
                onSecondaryContainer = accent.lightOnSecondaryContainer,
                tertiary = accent.lightTertiary,
                onTertiary = PaperSurface,
                tertiaryContainer = accent.lightTertiaryContainer,
                onTertiaryContainer = PaperTextPrimary,
                error = CrimsonLight,
                onError = PaperSurface,
                errorContainer = CrimsonLightContainer,
                onErrorContainer = CrimsonLightOnContainer,
                background = PaperBg,
                onBackground = PaperTextPrimary,
                surface = PaperSurface,
                onSurface = PaperTextPrimary,
                surfaceVariant = PaperCard,
                onSurfaceVariant = PaperTextSecondary,
                outline = PaperBorder,
                outlineVariant = PaperElevated
            )
        }
        themeMode == AppThemeMode.MIDNIGHT_CYBER -> {
            // Classic Obsidian Navy Cyber Dark Scheme
            darkColorScheme(
                primary = accent.darkPrimary,
                onPrimary = accent.darkOnPrimary,
                primaryContainer = accent.darkPrimaryContainer,
                onPrimaryContainer = accent.darkOnPrimaryContainer,
                secondary = accent.darkSecondary,
                onSecondary = accent.darkOnSecondary,
                secondaryContainer = accent.darkSecondaryContainer,
                onSecondaryContainer = accent.darkOnSecondaryContainer,
                tertiary = accent.darkTertiary,
                onTertiary = ObsidianDeep,
                tertiaryContainer = accent.darkTertiaryContainer,
                onTertiaryContainer = accent.darkTertiary,
                error = CrimsonOrb,
                onError = Color.White,
                errorContainer = CrimsonDarkContainer,
                onErrorContainer = CrimsonDarkOnContainer,
                background = ObsidianDeep,
                onBackground = TextPrimaryDark,
                surface = ObsidianSurface,
                onSurface = TextPrimaryDark,
                surfaceVariant = ObsidianCard,
                onSurfaceVariant = TextSecondaryDark,
                outline = ObsidianBorder,
                outlineVariant = ObsidianElevated
            )
        }
        else -> {
            // Default Dark Graphite Gray Scheme (also used for SYSTEM when system is dark)
            darkColorScheme(
                primary = accent.darkPrimary,
                onPrimary = accent.darkOnPrimary,
                primaryContainer = accent.darkPrimaryContainer,
                onPrimaryContainer = accent.darkOnPrimaryContainer,
                secondary = accent.darkSecondary,
                onSecondary = accent.darkOnSecondary,
                secondaryContainer = accent.darkSecondaryContainer,
                onSecondaryContainer = accent.darkOnSecondaryContainer,
                tertiary = accent.darkTertiary,
                onTertiary = GraphiteDeep,
                tertiaryContainer = accent.darkTertiaryContainer,
                onTertiaryContainer = accent.darkTertiary,
                error = CrimsonOrb,
                onError = Color.White,
                errorContainer = CrimsonDarkContainer,
                onErrorContainer = CrimsonDarkOnContainer,
                background = GraphiteDeep,
                onBackground = GraphiteTextPrimary,
                surface = GraphiteSurface,
                onSurface = GraphiteTextPrimary,
                surfaceVariant = GraphiteCard,
                onSurfaceVariant = GraphiteTextSecondary,
                outline = GraphiteBorder,
                outlineVariant = GraphiteElevated
            )
        }
    }

    return scheme to effectiveIsDark
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SerpentisTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK_GRAPHITE,
    accent: AppAccent = AppAccent.EMERALD_CYAN,
    useDynamicColors: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val (fallbackScheme, isDark) = buildSerpentisColorScheme(
        themeMode = themeMode,
        accent = accent,
        systemInDarkTheme = systemDark
    )

    val colorScheme = if (useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) {
            dynamicDarkColorScheme(context)
        } else {
            // Soften dynamic light background slightly so it remains gentle off-white on the eyes
            val dynLight = dynamicLightColorScheme(context)
            dynLight.copy(
                background = if (dynLight.background == Color.White) PaperBg else dynLight.background,
                surface = if (dynLight.surface == Color.White) PaperSurface else dynLight.surface
            )
        }
    } else {
        fallbackScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
