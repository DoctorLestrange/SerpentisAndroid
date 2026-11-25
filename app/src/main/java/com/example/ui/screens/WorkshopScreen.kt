package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppAccent
import com.example.model.AppThemeMode
import com.example.model.ArenaTheme
import com.example.model.BoardSize
import com.example.model.GameSettings
import com.example.model.SnakeSkin
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.GraphiteDeep
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.PaperBg
import com.example.ui.theme.PaperCard

@Composable
fun WorkshopScreen(
    settings: GameSettings,
    onSelectAppThemeMode: (AppThemeMode) -> Unit,
    onSelectAppAccent: (AppAccent) -> Unit,
    onSetUseDynamicColors: (Boolean) -> Unit,
    onSelectSkin: (SnakeSkin) -> Unit,
    onSelectArenaTheme: (ArenaTheme) -> Unit,
    onSelectBoardSize: (BoardSize) -> Unit,
    onSetFullAreaGrid: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val systemDark = isSystemInDarkTheme()
    val effectiveIsDark = when (settings.appThemeMode) {
        AppThemeMode.DARK_GRAPHITE, AppThemeMode.MIDNIGHT_CYBER -> true
        AppThemeMode.SOFT_LIGHT -> false
        AppThemeMode.SYSTEM -> systemDark
    }
    val supportsDynamicColors = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Live Interactive Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE ARENA PREVIEW",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${settings.snakeSkin.title} × ${settings.arenaTheme.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    LiveSkinArenaPreview(
                        skin = settings.snakeSkin,
                        arenaTheme = settings.arenaTheme,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }
            }

            // 2. App Theme & Accent Studio
            SectionHeader(
                icon = Icons.Filled.ColorLens,
                title = "APP INTERFACE THEME",
                subtitle = "Choose Dark Graphite, Midnight Cyber, Soft Off-White, or follow device"
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppThemeMode.entries.chunked(2).forEach { rowModes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowModes.forEach { mode ->
                            AppThemeModeCard(
                                mode = mode,
                                selected = settings.appThemeMode == mode,
                                onClick = { onSelectAppThemeMode(mode) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Follow Device Wallpaper Colors (Material You)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = if (settings.useDynamicColors) 2.dp else 1.dp,
                        color = if (settings.useDynamicColors) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSetUseDynamicColors(!settings.useDynamicColors) },
                color = if (settings.useDynamicColors) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Follow Device Theme Colors",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (supportsDynamicColors) {
                                    "Syncs Material You accent & surface palette with your wallpaper"
                                } else {
                                    "Requires Android 12+ (falls back gracefully to selected accent)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = settings.useDynamicColors,
                        onCheckedChange = onSetUseDynamicColors,
                        modifier = Modifier.testTag("workshop_dynamic_colors_toggle")
                    )
                }
            }

            // Custom Accent Palette Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CUSTOM ACCENT COLORS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    if (settings.useDynamicColors && supportsDynamicColors) {
                        Text(
                            text = "Tap any accent to override",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AppAccent.entries.chunked(2).forEach { rowAccents ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowAccents.forEach { accent ->
                            AccentOptionCard(
                                accent = accent,
                                isDark = effectiveIsDark,
                                selected = !settings.useDynamicColors && settings.appAccent == accent,
                                onClick = { onSelectAppAccent(accent) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3. Play Arena Themes Section
            SectionHeader(
                icon = Icons.Filled.Wallpaper,
                title = "PLAY ARENA THEMES",
                subtitle = "Includes Soft Paper (off-white), Graphite Matte (dark grey), Cyber & Retro LCD"
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ArenaTheme.entries.chunked(2).forEach { rowThemes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowThemes.forEach { theme ->
                            ArenaThemeCard(
                                theme = theme,
                                selected = settings.arenaTheme == theme,
                                onClick = { onSelectArenaTheme(theme) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4. Serpentis Skins Section
            SectionHeader(
                icon = Icons.Filled.Palette,
                title = "SERPENTIS CHASSIS SKINS",
                subtitle = "Customize head, scales gradient, and aura glow"
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SnakeSkin.entries.chunked(2).forEach { rowSkins ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowSkins.forEach { skin ->
                            SkinOptionCard(
                                skin = skin,
                                selected = settings.snakeSkin == skin,
                                onClick = { onSelectSkin(skin) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 5. Grid Dimensions Section
            SectionHeader(
                icon = Icons.Filled.GridOn,
                title = "ARENA GRID DIMENSIONS",
                subtitle = "Adjust tactical board capacity"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BoardSize.entries.forEach { size ->
                    val selected = settings.boardSize == size
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectBoardSize(size) }
                            .testTag("board_size_${size.name.lowercase()}"),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${size.columns}×${size.rows}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = size.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = if (settings.fullAreaGrid) 2.dp else 1.dp,
                        color = if (settings.fullAreaGrid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSetFullAreaGrid(!settings.fullAreaGrid) }
                    .testTag("workshop_full_area_toggle"),
                color = if (settings.fullAreaGrid) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Use Full Available Play Area",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Expand the play grid to fill the entire space between the header and bottom bar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (settings.fullAreaGrid) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Full-Area Grid Enabled",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AppThemeModeCard(
    mode: AppThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgSwatch, surfaceSwatch) = when (mode) {
        AppThemeMode.DARK_GRAPHITE -> GraphiteDeep to GraphiteCard
        AppThemeMode.MIDNIGHT_CYBER -> ObsidianDeep to ObsidianCard
        AppThemeMode.SOFT_LIGHT -> PaperBg to PaperCard
        AppThemeMode.SYSTEM -> GraphiteDeep to PaperBg
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .testTag("app_theme_${mode.name.lowercase()}"),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SwatchCircle(bgSwatch)
                    SwatchCircle(surfaceSwatch)
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = mode.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = mode.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AccentOptionCard(
    accent: AppAccent,
    isDark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (isDark) accent.darkPrimary else accent.lightPrimary
    val secondaryColor = if (isDark) accent.darkSecondary else accent.lightSecondary
    val tertiaryColor = if (isDark) accent.darkTertiary else accent.lightTertiary

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) primaryColor else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .testTag("accent_card_${accent.name.lowercase()}"),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    SwatchCircle(primaryColor)
                    SwatchCircle(secondaryColor)
                    SwatchCircle(tertiaryColor)
                }
                Text(
                    text = accent.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun LiveSkinArenaPreview(
    skin: SnakeSkin,
    arenaTheme: ArenaTheme,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(arenaTheme.boardBgStart, arenaTheme.boardBgEnd)),
            size = size,
            cornerRadius = CornerRadius(24f, 24f)
        )

        val cols = 12
        val rows = 4
        val cellW = size.width / cols
        val cellH = size.height / rows
        val cellMin = minOf(cellW, cellH)

        for (x in 1 until cols) {
            drawLine(
                color = arenaTheme.gridLineColor.copy(alpha = 0.55f),
                start = Offset(x * cellW, 0f),
                end = Offset(x * cellW, size.height),
                strokeWidth = 1.5f
            )
        }
        for (y in 1 until rows) {
            drawLine(
                color = arenaTheme.gridLineColor.copy(alpha = 0.55f),
                start = Offset(0f, y * cellH),
                end = Offset(size.width, y * cellH),
                strokeWidth = 1.5f
            )
        }

        // Draw preview snake of 6 segments
        val segCount = 6
        for (i in (segCount - 1) downTo 0) {
            val gx = 7 - i
            val gy = 2
            val isHead = (i == 0)
            val t = i.toFloat() / (segCount - 1).toFloat()
            val c = when {
                arenaTheme.isRetroLcd -> Color(0xFF0F380F)
                isHead -> skin.headColor
                else -> lerp(skin.bodyStartColor, skin.bodyEndColor, t)
            }
            val topLeft = Offset(gx * cellW + cellW * 0.08f, (gy - 0.5f) * cellH + cellH * 0.08f)
            val segSize = Size(cellW * 0.84f, cellH * 0.84f)
            drawRoundRect(
                color = c,
                topLeft = topLeft,
                size = segSize,
                cornerRadius = CornerRadius(cellMin * 0.28f, cellMin * 0.28f)
            )
            if (isHead) {
                val eyeColor = if (arenaTheme.isRetroLcd) arenaTheme.boardBgStart else skin.eyeColor
                val cx = (gx + 0.5f) * cellW
                val cy = 2f * cellH
                drawCircle(
                    color = eyeColor,
                    radius = (cellMin * 0.09f).coerceAtLeast(2f),
                    center = Offset(cx + cellMin * 0.16f, cy - cellMin * 0.16f)
                )
                drawCircle(
                    color = eyeColor,
                    radius = (cellMin * 0.09f).coerceAtLeast(2f),
                    center = Offset(cx + cellMin * 0.16f, cy + cellMin * 0.16f)
                )
            }
        }

        // Preview apple orb
        val orbCenter = Offset(9.5f * cellW, 2f * cellH)
        val orbColor = when {
            arenaTheme.isRetroLcd -> Color(0xFF0F380F)
            arenaTheme.isLightBoard -> Color(0xFFD91E48)
            else -> Color(0xFFFF2A6D)
        }
        drawCircle(
            color = orbColor,
            radius = cellMin * 0.34f,
            center = orbCenter
        )

        drawRoundRect(
            color = arenaTheme.wallColor,
            size = size,
            cornerRadius = CornerRadius(24f, 24f),
            style = Stroke(width = 4f)
        )
    }
}

@Composable
private fun SkinOptionCard(
    skin: SnakeSkin,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .testTag("skin_card_${skin.name.lowercase()}"),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SwatchCircle(skin.headColor)
                    SwatchCircle(skin.bodyStartColor)
                    SwatchCircle(skin.bodyEndColor)
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = skin.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = skin.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ArenaThemeCard(
    theme: ArenaTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .testTag("theme_card_${theme.name.lowercase()}"),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SwatchCircle(theme.boardBgStart)
                    SwatchCircle(theme.recommendedSkin?.headColor ?: theme.wallColor)
                    SwatchCircle(theme.obstacleColor)
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = theme.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = theme.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SwatchCircle(color: Color) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), CircleShape)
    )
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
