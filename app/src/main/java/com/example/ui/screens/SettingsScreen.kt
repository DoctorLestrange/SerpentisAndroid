package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppAccent
import com.example.model.AppThemeMode
import com.example.model.ControlScheme
import com.example.model.GameSettings
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: GameSettings,
    onSelectAppThemeMode: (AppThemeMode) -> Unit,
    onSelectAppAccent: (AppAccent) -> Unit,
    onSetUseDynamicColors: (Boolean) -> Unit,
    onSelectControlScheme: (ControlScheme) -> Unit,
    onSetSwipeSensitivity: (Float) -> Unit,
    onSetDynamicSpeedScaling: (Boolean) -> Unit,
    onSetSmoothMovement: (Boolean) -> Unit,
    onSetShowGridLines: (Boolean) -> Unit,
    onSetFullAreaGrid: (Boolean) -> Unit,
    onSetPoisonEnabled: (Boolean) -> Unit,
    onSetSoundEnabled: (Boolean) -> Unit,
    onSetHapticsEnabled: (Boolean) -> Unit,
    onTestFeedback: () -> Unit,
    onResetDefaults: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. App Appearance & Device Theme Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ColorLens,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "APP THEME & ACCENT COLORS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.appThemeMode == mode,
                                onClick = { onSelectAppThemeMode(mode) },
                                label = {
                                    Text(
                                        text = mode.title,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                modifier = Modifier.testTag("settings_theme_${mode.name.lowercase()}")
                            )
                        }
                    }

                    SettingsToggleRow(
                        title = "Follow Device Theme Colors",
                        subtitle = if (supportsDynamicColors) {
                            "Match Material You dynamic colors from your device wallpaper"
                        } else {
                            "Available on Android 12+; uses your selected accent palette on older devices"
                        },
                        checked = settings.useDynamicColors,
                        testTag = "toggle_dynamic_colors",
                        onCheckedChange = onSetUseDynamicColors
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (settings.useDynamicColors && supportsDynamicColors) {
                                "Accent Color (Tap any swatch to customize)"
                            } else {
                                "Accent Color: ${settings.appAccent.title}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppAccent.entries.forEach { accent ->
                                val swatchColor = if (effectiveIsDark) accent.darkPrimary else accent.lightPrimary
                                val iconTint = if (effectiveIsDark) accent.darkOnPrimary else accent.lightOnPrimary
                                val isSelected = !settings.useDynamicColors && settings.appAccent == accent
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .minimumInteractiveComponentSize()
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            },
                                            shape = CircleShape
                                        )
                                        .semantics {
                                            role = Role.Button
                                            contentDescription = "Accent ${accent.title}"
                                        }
                                        .clickable { onSelectAppAccent(accent) }
                                        .testTag("settings_accent_${accent.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = iconTint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 1. Control Scheme Picker
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Gamepad,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "ERGONOMIC CONTROL SCHEME",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ControlScheme.entries.forEach { scheme ->
                    val selected = settings.controlScheme == scheme
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectControlScheme(scheme) }
                            .testTag("control_scheme_${scheme.name.lowercase()}"),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
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
                                    text = scheme.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = scheme.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Engine & Visual Calibration
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ENGINE & DISPLAY CALIBRATION",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Swipe threshold slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Swipe Gesture Distance",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            val pct = ((2.0f - settings.swipeSensitivity) * 100).roundToInt()
                            Text(
                                text = "${pct}% Sensitivity",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = settings.swipeSensitivity,
                            onValueChange = onSetSwipeSensitivity,
                            valueRange = 0.5f..1.5f,
                            modifier = Modifier.testTag("swipe_sensitivity_slider")
                        )
                    }

                    SettingsToggleRow(
                        title = "Full-Area Play Grid",
                        subtitle = "Use all available space between the header and bottom bar for the play grid",
                        checked = settings.fullAreaGrid,
                        testTag = "toggle_full_area_grid",
                        onCheckedChange = onSetFullAreaGrid
                    )

                    SettingsToggleRow(
                        title = "Poison Skull Hazards (☠)",
                        subtitle = "Spawns a periodic fatal skull away from the serpent head (Default ON in Hyper & Overclocked)",
                        checked = settings.poisonEnabled,
                        testTag = "toggle_poison_hazard",
                        onCheckedChange = onSetPoisonEnabled
                    )

                    SettingsToggleRow(
                        title = "Dynamic Speed Scaling",
                        subtitle = "Gradually accelerate tick rate as your serpent grows longer",
                        checked = settings.dynamicSpeedScaling,
                        testTag = "toggle_dynamic_speed",
                        onCheckedChange = onSetDynamicSpeedScaling
                    )

                    SettingsToggleRow(
                        title = "Smooth 60fps Interpolation",
                        subtitle = "Render continuous sub-tick motion between grid cells",
                        checked = settings.smoothMovement,
                        testTag = "toggle_smooth_movement",
                        onCheckedChange = onSetSmoothMovement
                    )

                    SettingsToggleRow(
                        title = "Show Tactical Grid Lines",
                        subtitle = "Display cell coordinate grid inside the arena",
                        checked = settings.showGridLines,
                        testTag = "toggle_grid_lines",
                        onCheckedChange = onSetShowGridLines
                    )
                }
            }

            // 3. Audio & Haptics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SYNTHESIZED AUDIO & HAPTICS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    SettingsToggleRow(
                        title = "Arcade Chiptune Sound Effects",
                        subtitle = "Real-time synthesized waveform audio on bites, warps, and combos",
                        checked = settings.soundEnabled,
                        testTag = "toggle_sound_fx",
                        onCheckedChange = onSetSoundEnabled
                    )

                    SettingsToggleRow(
                        title = "Tactile Haptic Vibration",
                        subtitle = "Crisp physical pulse feedback on turns and orb pickups",
                        checked = settings.hapticsEnabled,
                        testTag = "toggle_haptics",
                        onCheckedChange = onSetHapticsEnabled
                    )

                    OutlinedButton(
                        onClick = onTestFeedback,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("test_audio_haptics_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("TEST SOUND & HAPTIC PULSE")
                    }
                }
            }

            // 4. Reset Defaults
            OutlinedButton(
                onClick = onResetDefaults,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("reset_settings_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Filled.RestartAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.reset_defaults))
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
