package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ActiveBuff
import com.example.model.Direction
import com.example.model.GameSettings
import com.example.model.GameState
import com.example.model.GameStatus
import com.example.ui.components.GameControlsBar
import com.example.ui.components.SnakeBoardCanvas
import kotlin.math.abs

@Composable
fun ArenaScreen(
    gameState: GameState,
    settings: GameSettings,
    subTickFraction: Float,
    onDirectionInput: (Direction) -> Unit,
    onRelativeTurnInput: (Boolean) -> Unit,
    onStartOrResume: () -> Unit,
    onTogglePause: () -> Unit,
    onRestartGame: () -> Unit,
    onToggleFullAreaGrid: () -> Unit,
    onPlayAreaSizeChanged: (Int, Int) -> Unit,
    onExitToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        if (gameState.status == GameStatus.RUNNING) {
            onTogglePause()
        } else {
            onExitToHome()
        }
    }

    val density = LocalDensity.current
    val sensitivityFactor = settings.swipeSensitivity.coerceIn(0.5f, 1.5f)
    val firstThresholdPx = with(density) { (18.dp * sensitivityFactor).toPx() }
    val secondThresholdPx = with(density) { (22.dp * sensitivityFactor).toPx() }

    val currentStatus by rememberUpdatedState(gameState.status)
    val currentOnDirectionInput by rememberUpdatedState(onDirectionInput)
    val currentOnStartOrResume by rememberUpdatedState(onStartOrResume)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = if (settings.fullAreaGrid) 4.dp else 12.dp,
                vertical = if (settings.fullAreaGrid) 4.dp else 8.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Fixed-Height Tactical HUD Header (integrates buffs & blitz timer so play grid never shifts or resizes)
        Box(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
        ) {
            ArenaHudHeader(
                gameState = gameState,
                fullAreaGrid = settings.fullAreaGrid,
                poisonEnabled = settings.poisonEnabled,
                onToggleFullAreaGrid = onToggleFullAreaGrid,
                onTogglePause = onTogglePause,
                onRestart = onRestartGame
            )
        }

        // 2. Unified Interactive Play & Outer-Swipe Region
        // Covers the entire space below the header and above the bottom navigation bar.
        // Control buttons and modal cards consume their own pointer events first,
        // so swipes anywhere inside OR outside the play grid steer the snake.
        // Restricted to at most 2 perpendicular moves per continuous gesture (L-swipe)
        // with cross-axis drift filtering so single swipes and L-swipes never trigger a 3rd self-biting turn.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 4.dp)
                .testTag("arena_swipe_region")
                .pointerInput(firstThresholdPx, secondThresholdPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        var accumDx = 0f
                        var accumDy = 0f
                        var totalMove = 0f
                        var swipesInGesture = 0
                        var firstSwipeDir: Direction? = null

                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.isConsumed }) {
                                break
                            }
                            val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                            if (change != null && change.pressed) {
                                val delta = change.positionChange()
                                totalMove += abs(delta.x) + abs(delta.y)

                                if ((currentStatus == GameStatus.RUNNING || currentStatus == GameStatus.READY) &&
                                    swipesInGesture < 2
                                ) {
                                    if (swipesInGesture == 0) {
                                        accumDx += delta.x
                                        accumDy += delta.y
                                        if (abs(accumDx) >= firstThresholdPx || abs(accumDy) >= firstThresholdPx) {
                                            val dir1 = if (abs(accumDx) > abs(accumDy)) {
                                                if (accumDx > 0f) Direction.RIGHT else Direction.LEFT
                                            } else {
                                                if (accumDy > 0f) Direction.DOWN else Direction.UP
                                            }
                                            currentOnDirectionInput(dir1)
                                            firstSwipeDir = dir1
                                            swipesInGesture = 1
                                            accumDx = 0f
                                            accumDy = 0f
                                            change.consume()
                                        }
                                    } else {
                                        // 2nd leg of an L-swipe: only accumulate movement perpendicular to firstSwipeDir,
                                        // bleeding off cross-axis drift while moving along the first leg.
                                        val firstWasVertical =
                                            firstSwipeDir == Direction.UP || firstSwipeDir == Direction.DOWN
                                        if (firstWasVertical) {
                                            if (abs(delta.x) >= abs(delta.y) * 0.85f) {
                                                accumDx += delta.x
                                            } else {
                                                accumDx *= 0.3f
                                            }
                                            if (abs(accumDx) >= secondThresholdPx) {
                                                val dir2 = if (accumDx > 0f) Direction.RIGHT else Direction.LEFT
                                                currentOnDirectionInput(dir2)
                                                swipesInGesture = 2
                                                accumDx = 0f
                                                accumDy = 0f
                                                change.consume()
                                            }
                                        } else {
                                            if (abs(delta.y) >= abs(delta.x) * 0.85f) {
                                                accumDy += delta.y
                                            } else {
                                                accumDy *= 0.3f
                                            }
                                            if (abs(accumDy) >= secondThresholdPx) {
                                                val dir2 = if (accumDy > 0f) Direction.DOWN else Direction.UP
                                                currentOnDirectionInput(dir2)
                                                swipesInGesture = 2
                                                accumDx = 0f
                                                accumDy = 0f
                                                change.consume()
                                            }
                                        }
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        if (swipesInGesture == 0 && totalMove < viewConfiguration.touchSlop) {
                            if (currentStatus == GameStatus.READY) {
                                currentOnStartOrResume()
                            }
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Center Arena Play Area + Status Overlays
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        onPlayAreaSizeChanged(size.width, size.height)
                    },
                contentAlignment = Alignment.Center
            ) {
                SnakeBoardCanvas(
                    gameState = gameState,
                    skin = settings.snakeSkin,
                    arenaTheme = settings.arenaTheme,
                    showGridLines = settings.showGridLines,
                    smoothMovement = settings.smoothMovement,
                    fullAreaGrid = settings.fullAreaGrid,
                    subTickFraction = subTickFraction,
                    modifier = Modifier.fillMaxSize()
                )

                // Ready Banner Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = gameState.status == GameStatus.READY,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ReadyLaunchOverlay(
                        modeTitle = gameState.mode.title,
                        difficultyTitle = gameState.difficulty.title,
                        onLaunch = onStartOrResume
                    )
                }

                // Pause Modal Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = gameState.status == GameStatus.PAUSED,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    PausedOverlayCard(
                        gameState = gameState,
                        onResume = onStartOrResume,
                        onRestart = onRestartGame,
                        onExit = onExitToHome
                    )
                }

                // Game Over / Victory / Time Up Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = gameState.status == GameStatus.GAME_OVER ||
                        gameState.status == GameStatus.VICTORY ||
                        gameState.status == GameStatus.TIME_UP,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    RunSummaryOverlayCard(
                        gameState = gameState,
                        onPlayAgain = onRestartGame,
                        onExit = onExitToHome
                    )
                }
            }

            // 3. Bottom Ergonomic Controls (buttons consume their own touches; empty space around them allows swipes)
            Box(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
            ) {
                GameControlsBar(
                    controlScheme = settings.controlScheme,
                    gameStatus = gameState.status,
                    onDirection = onDirectionInput,
                    onRelativeTurn = onRelativeTurnInput,
                    onTogglePause = onTogglePause,
                    modifier = Modifier.padding(
                        top = if (settings.fullAreaGrid) 2.dp else 6.dp,
                        bottom = if (settings.fullAreaGrid) 2.dp else 4.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun ArenaHudHeader(
    gameState: GameState,
    fullAreaGrid: Boolean,
    poisonEnabled: Boolean,
    onToggleFullAreaGrid: () -> Unit,
    onTogglePause: () -> Unit,
    onRestart: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("arena_hud_header"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Top Row: Fixed 46.dp height so score/combo changes never alter header height
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column {
                        Text(
                            text = "SCORE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = "${gameState.score}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.testTag("hud_score_text")
                        )
                    }

                    Column {
                        Text(
                            text = "BEST",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = "${gameState.highScoreForMode}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    Column {
                        Text(
                            text = "LEN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = "${gameState.snakeLength}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (gameState.comboCount > 1) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.tertiary,
                                RoundedCornerShape(8.dp)
                            )
                        ) {
                            Text(
                                text = "${gameState.comboCount}x",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleFullAreaGrid,
                        modifier = Modifier.testTag("hud_full_area_button")
                    ) {
                        Icon(
                            imageVector = if (fullAreaGrid) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            contentDescription = stringResource(R.string.toggle_full_area_grid),
                            tint = if (fullAreaGrid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onTogglePause,
                        modifier = Modifier.testTag("hud_pause_button")
                    ) {
                        Icon(
                            imageVector = if (gameState.status == GameStatus.RUNNING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.pause_game),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onRestart,
                        modifier = Modifier.testTag("hud_restart_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.restart_game),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bottom Strip: Strictly fixed 24.dp height!
            // Displays Active Buffs (e.g., 2x Overdrive, Phase Ghost, Chrono Shard) and/or Blitz Timer
            // without ever shifting or resizing the play grid below!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val hasBuffs = gameState.activeBuffs.isNotEmpty()
                val isBlitz = gameState.mode.isTimedBlitz

                if (hasBuffs || isBlitz) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isBlitz) {
                            CompactBlitzTimerChip(
                                remainingMillis = gameState.remainingBlitzMillis,
                                modifier = Modifier.weight(if (hasBuffs) 0.9f else 1f)
                            )
                        }
                        gameState.activeBuffs.forEach { buff ->
                            CompactBuffChip(
                                buff = buff,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    // Ambient tactical telemetry strip when no buffs are active (keeps height 100% constant)
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val poisonBadge = if (poisonEnabled) " • ☠ POISON" else ""
                        Text(
                            text = "${gameState.mode.title.uppercase()} • ${gameState.difficulty.title.uppercase()}$poisonBadge",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (gameState.comboCount > 1) {
                            val comboRem = (1f - (gameState.ticksSinceLastEat.toFloat() / gameState.comboWindowTicks.coerceAtLeast(1).toFloat()))
                                .coerceIn(0f, 1f)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${gameState.comboCount}x CHAIN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                LinearProgressIndicator(
                                    progress = { comboRem },
                                    modifier = Modifier
                                        .width(56.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "${gameState.boardWidth}×${gameState.boardHeight} GRID",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactBlitzTimerChip(
    remainingMillis: Long,
    modifier: Modifier = Modifier
) {
    val totalSeconds = (remainingMillis / 1000L).coerceAtLeast(0L)
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    val progress = (remainingMillis.toFloat() / 120_000f).coerceIn(0f, 1f)
    val barColor = if (totalSeconds <= 15) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary

    Row(
        modifier = modifier
            .height(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Timer,
            contentDescription = null,
            tint = barColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = "%d:%02d".format(mins, secs),
            style = MaterialTheme.typography.labelSmall,
            color = barColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun CompactBuffChip(
    buff: ActiveBuff,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .border(1.dp, buff.type.color.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "${buff.type.symbol} ${buff.type.title}",
            style = MaterialTheme.typography.labelSmall,
            color = buff.type.color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        LinearProgressIndicator(
            progress = { buff.progress },
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = buff.type.color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = "${buff.remainingTicks}t",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun ReadyLaunchOverlay(
    modeTitle: String,
    difficultyTitle: String,
    onLaunch: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(24.dp)
            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = modeTitle.uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Speed Tier: $difficultyTitle • Tap or Swipe Anywhere to Begin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onLaunch,
                modifier = Modifier.testTag("arena_ready_start_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ENGAGE SERPENTIS", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PausedOverlayCard(
    gameState: GameState,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Card(
        modifier = Modifier
            .widthIn(max = 340.dp)
            .padding(20.dp)
            .border(1.5.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "SYSTEM PAUSED",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "Score: ${gameState.score} • Length: ${gameState.snakeLength}\nProgress automatically saved",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pause_resume_button")
            ) {
                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.resume_game), fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pause_restart_button")
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.restart_game))
            }

            OutlinedButton(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pause_exit_button")
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save & Exit to Arcade")
            }
        }
    }
}

@Composable
private fun RunSummaryOverlayCard(
    gameState: GameState,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val isVictory = gameState.status == GameStatus.VICTORY
    val isTimeUp = gameState.status == GameStatus.TIME_UP
    val accentColor = when {
        isVictory -> MaterialTheme.colorScheme.primary
        isTimeUp -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
    val headerTitle = when {
        isVictory -> stringResource(R.string.board_clear_title)
        isTimeUp -> stringResource(R.string.time_up_title)
        else -> stringResource(R.string.game_over_title)
    }

    Card(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .padding(16.dp)
            .border(2.dp, accentColor, RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = headerTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = accentColor,
                textAlign = TextAlign.Center
            )

            if (gameState.deathReason.message.isNotEmpty()) {
                Text(
                    text = gameState.deathReason.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (gameState.isNewHighScore) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.new_high_score),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Text(
                text = "${gameState.score}",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStatColumn("APPLES", "${gameState.applesEaten}")
                SummaryStatColumn("MAX COMBO", "${gameState.maxComboReached}x")
                SummaryStatColumn("LENGTH", "${gameState.snakeLength}")
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("game_over_play_again_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.restart_game), fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("game_over_exit_button")
            ) {
                Text(stringResource(R.string.quit_to_menu))
            }
        }
    }
}

@Composable
private fun SummaryStatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
