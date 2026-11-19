package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.ControlScheme
import com.example.model.Direction
import com.example.model.GameStatus

@Composable
fun GameControlsBar(
    controlScheme: ControlScheme,
    gameStatus: GameStatus,
    onDirection: (Direction) -> Unit,
    onRelativeTurn: (Boolean) -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (controlScheme) {
        ControlScheme.DPAD -> DPadControls(
            gameStatus = gameStatus,
            onDirection = onDirection,
            onTogglePause = onTogglePause,
            modifier = modifier
        )
        ControlScheme.SPLIT_BUTTONS -> SplitThumbControls(
            onDirection = onDirection,
            modifier = modifier
        )
        ControlScheme.RELATIVE_STEER -> RelativeSteerControls(
            onRelativeTurn = onRelativeTurn,
            modifier = modifier
        )
        ControlScheme.SWIPE_ONLY -> {
            // No bottom control bar in SWIPE_ONLY mode so the play grid can use the full area down to the bottom bar
        }
    }
}

@Composable
private fun DPadControls(
    gameStatus: GameStatus,
    onDirection: (Direction) -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier
) {
    var centerPressed by remember { mutableStateOf(false) }
    val pauseDesc = if (gameStatus == GameStatus.RUNNING) "Pause Game" else "Resume Game"

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ArcadePadButton(
            icon = Icons.Filled.KeyboardArrowUp,
            contentDesc = "Move Up",
            testTag = "dpad_up",
            onPress = { onDirection(Direction.UP) }
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ArcadePadButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDesc = "Move Left",
                testTag = "dpad_left",
                onPress = { onDirection(Direction.LEFT) }
            )
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(
                        if (centerPressed) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .semantics {
                        role = Role.Button
                        contentDescription = pauseDesc
                        onClick {
                            onTogglePause()
                            true
                        }
                    }
                    .pointerInput(onTogglePause) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            centerPressed = true
                            onTogglePause()
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                            centerPressed = false
                        }
                    }
                    .testTag("dpad_center_pause"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (gameStatus == GameStatus.RUNNING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            ArcadePadButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDesc = "Move Right",
                testTag = "dpad_right",
                onPress = { onDirection(Direction.RIGHT) }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        ArcadePadButton(
            icon = Icons.Filled.KeyboardArrowDown,
            contentDesc = "Move Down",
            testTag = "dpad_down",
            onPress = { onDirection(Direction.DOWN) }
        )
    }
}

@Composable
private fun SplitThumbControls(
    onDirection: (Direction) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ArcadePadButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDesc = "Move Left",
                testTag = "split_left",
                width = 72.dp,
                height = 68.dp,
                onPress = { onDirection(Direction.LEFT) }
            )
            ArcadePadButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDesc = "Move Right",
                testTag = "split_right",
                width = 72.dp,
                height = 68.dp,
                onPress = { onDirection(Direction.RIGHT) }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ArcadePadButton(
                icon = Icons.Filled.KeyboardArrowUp,
                contentDesc = "Move Up",
                testTag = "split_up",
                width = 88.dp,
                height = 54.dp,
                onPress = { onDirection(Direction.UP) }
            )
            ArcadePadButton(
                icon = Icons.Filled.KeyboardArrowDown,
                contentDesc = "Move Down",
                testTag = "split_down",
                width = 88.dp,
                height = 54.dp,
                onPress = { onDirection(Direction.DOWN) }
            )
        }
    }
}

@Composable
private fun RelativeSteerControls(
    onRelativeTurn: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InstantSteerPaddle(
            label = "TURN LEFT",
            icon = Icons.AutoMirrored.Filled.RotateLeft,
            isRight = false,
            testTag = "steer_ccw_button",
            onPress = { onRelativeTurn(false) },
            modifier = Modifier.weight(1f)
        )
        InstantSteerPaddle(
            label = "TURN RIGHT",
            icon = Icons.AutoMirrored.Filled.RotateRight,
            isRight = true,
            testTag = "steer_cw_button",
            onPress = { onRelativeTurn(true) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InstantSteerPaddle(
    label: String,
    icon: ImageVector,
    isRight: Boolean,
    testTag: String,
    onPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val accent = if (isRight) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .height(68.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(18.dp))
            .semantics {
                role = Role.Button
                contentDescription = label
                onClick {
                    onPress()
                    true
                }
            }
            .pointerInput(onPress) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    onPress()
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    isPressed = false
                }
            }
            .testTag(testTag),
        color = if (isPressed) accent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .border(
                    width = if (isPressed) 2.5.dp else 1.5.dp,
                    color = accent.copy(alpha = if (isPressed) 1f else 0.6f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (!isRight) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            if (isRight) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun ArcadePadButton(
    icon: ImageVector,
    contentDesc: String,
    testTag: String,
    width: Dp = 74.dp,
    height: Dp = 54.dp,
    onPress: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isPressed) primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (isPressed) 2.5.dp else 1.5.dp,
                color = primary.copy(alpha = if (isPressed) 0.95f else 0.45f),
                shape = RoundedCornerShape(16.dp)
            )
            .semantics {
                role = Role.Button
                contentDescription = contentDesc
                onClick {
                    onPress()
                    true
                }
            }
            .pointerInput(onPress) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    onPress()
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    isPressed = false
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPressed) primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(32.dp)
        )
    }
}
