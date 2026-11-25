package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.ArenaTheme
import com.example.model.Direction
import com.example.model.FoodType
import com.example.model.GameState
import com.example.model.GameStatus
import com.example.model.SnakeSkin
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun SnakeBoardCanvas(
    gameState: GameState,
    skin: SnakeSkin,
    arenaTheme: ArenaTheme,
    fullAreaGrid: Boolean,
    showGridLines: Boolean,
    smoothMovement: Boolean,
    subTickFraction: Float,
    modifier: Modifier = Modifier
) {
    val aspectRatio = remember(gameState.boardWidth, gameState.boardHeight) {
        gameState.boardWidth.toFloat() / gameState.boardHeight.toFloat().coerceAtLeast(1f)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val containerAspect = if (maxHeight > 0.dp) maxWidth / maxHeight else 1f
            val fittedCanvasModifier = if (fullAreaGrid) {
                Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .testTag("snake_board_canvas")
            } else {
                Modifier
                    .aspectRatio(
                        ratio = aspectRatio,
                        matchHeightConstraintsFirst = containerAspect > aspectRatio
                    )
                    .fillMaxSize()
                    .padding(4.dp)
                    .testTag("snake_board_canvas")
            }

            Canvas(modifier = fittedCanvasModifier) {
                val cols = gameState.boardWidth.coerceAtLeast(1)
                val rows = gameState.boardHeight.coerceAtLeast(1)
                val cellW = size.width / cols
                val cellH = size.height / rows
                val cellMin = min(cellW, cellH)

                // 1. Draw Arena Background
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(arenaTheme.boardBgStart, arenaTheme.boardBgEnd)
                    ),
                    cornerRadius = CornerRadius(cellMin * 0.45f, cellMin * 0.45f),
                    size = size
                )

                // 2. Draw Grid Lines or LCD Matrix Cells
                if (showGridLines) {
                    if (arenaTheme.isRetroLcd) {
                        for (x in 0 until cols) {
                            for (y in 0 until rows) {
                                drawRect(
                                    color = arenaTheme.gridLineColor.copy(alpha = 0.28f),
                                    topLeft = Offset(x * cellW + cellW * 0.08f, y * cellH + cellH * 0.08f),
                                    size = Size(cellW * 0.84f, cellH * 0.84f)
                                )
                            }
                        }
                    } else {
                        val strokeW = (cellMin * 0.04f).coerceAtLeast(1f)
                        for (x in 1 until cols) {
                            val px = x * cellW
                            drawLine(
                                color = arenaTheme.gridLineColor.copy(alpha = 0.55f),
                                start = Offset(px, 0f),
                                end = Offset(px, size.height),
                                strokeWidth = strokeW
                            )
                        }
                        for (y in 1 until rows) {
                            val py = y * cellH
                            drawLine(
                                color = arenaTheme.gridLineColor.copy(alpha = 0.55f),
                                start = Offset(0f, py),
                                end = Offset(size.width, py),
                                strokeWidth = strokeW
                            )
                        }
                    }
                }

                // 3. Draw Portals & Tether Lines
                for (portal in gameState.portals) {
                    val pColor = Color(portal.colorHex)
                    val centerA = Offset((portal.entryA.x + 0.5f) * cellW, (portal.entryA.y + 0.5f) * cellH)
                    val centerB = Offset((portal.entryB.x + 0.5f) * cellW, (portal.entryB.y + 0.5f) * cellH)

                    drawLine(
                        color = pColor.copy(alpha = 0.16f),
                        start = centerA,
                        end = centerB,
                        strokeWidth = cellMin * 0.08f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(cellMin * 0.3f, cellMin * 0.3f))
                    )
                    drawPortalNode(centerA, cellMin, pColor)
                    drawPortalNode(centerB, cellMin, pColor)
                }

                // 4. Draw Obstacles
                val phaseGhost = gameState.isPhaseGhostActive
                val obstacleAlpha = if (phaseGhost) 0.42f else 1.0f
                for (obs in gameState.obstacles) {
                    val topLeft = Offset(obs.x * cellW + cellW * 0.06f, obs.y * cellH + cellH * 0.06f)
                    val blockSize = Size(cellW * 0.88f, cellH * 0.88f)
                    drawRoundRect(
                        color = arenaTheme.obstacleColor.copy(alpha = obstacleAlpha),
                        topLeft = topLeft,
                        size = blockSize,
                        cornerRadius = CornerRadius(cellMin * 0.2f, cellMin * 0.2f)
                    )
                    drawRoundRect(
                        color = arenaTheme.wallColor.copy(alpha = 0.45f * obstacleAlpha),
                        topLeft = topLeft,
                        size = blockSize,
                        cornerRadius = CornerRadius(cellMin * 0.2f, cellMin * 0.2f),
                        style = Stroke(width = cellMin * 0.06f)
                    )
                }

                // 5. Draw Food, Magnet Power-Up & Poison Skull Hazard
                for (food in gameState.foods) {
                    val center = Offset((food.position.x + 0.5f) * cellW, (food.position.y + 0.5f) * cellH)
                    val fColor = when {
                        arenaTheme.isRetroLcd -> Color(0xFF0F380F)
                        arenaTheme.isLightBoard -> lightBoardFoodColor(food.type)
                        else -> food.type.color
                    }

                    when (food.type) {
                        FoodType.POISON_SKULL -> {
                            drawPoisonSkull(
                                center = center,
                                cellMin = cellMin,
                                isRetroLcd = arenaTheme.isRetroLcd,
                                isLightBoard = arenaTheme.isLightBoard,
                                boardBg = arenaTheme.boardBgStart
                            )
                        }
                        FoodType.MAGNET_CORE -> {
                            drawMagnetOrb(
                                center = center,
                                cellMin = cellMin,
                                isRetroLcd = arenaTheme.isRetroLcd,
                                isLightBoard = arenaTheme.isLightBoard
                            )
                        }
                        FoodType.PHASE_GHOST, FoodType.CHRONO_SHARD -> {
                            if (!arenaTheme.isRetroLcd) {
                                drawCircle(
                                    color = fColor.copy(alpha = if (arenaTheme.isLightBoard) 0.16f else 0.25f),
                                    radius = cellMin * 0.48f,
                                    center = center
                                )
                            }
                            val r = cellMin * 0.38f
                            val diamond = Path().apply {
                                moveTo(center.x, center.y - r)
                                lineTo(center.x + r, center.y)
                                lineTo(center.x, center.y + r)
                                lineTo(center.x - r, center.y)
                                close()
                            }
                            drawPath(path = diamond, color = fColor)
                            if (!arenaTheme.isRetroLcd) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.7f),
                                    radius = cellMin * 0.10f,
                                    center = Offset(center.x - cellMin * 0.09f, center.y - cellMin * 0.09f)
                                )
                            }
                        }
                        FoodType.GOLDEN_CORE -> {
                            if (!arenaTheme.isRetroLcd) {
                                drawCircle(
                                    color = fColor.copy(alpha = if (arenaTheme.isLightBoard) 0.18f else 0.25f),
                                    radius = cellMin * 0.48f,
                                    center = center
                                )
                            }
                            drawCircle(color = fColor, radius = cellMin * 0.36f, center = center)
                            drawCircle(
                                color = if (arenaTheme.isLightBoard) Color(0xFFFFF8E1) else Color.White,
                                radius = cellMin * 0.20f,
                                center = center,
                                style = Stroke(width = cellMin * 0.06f)
                            )
                        }
                        else -> {
                            if (!arenaTheme.isRetroLcd) {
                                drawCircle(
                                    color = fColor.copy(alpha = if (arenaTheme.isLightBoard) 0.16f else 0.25f),
                                    radius = cellMin * 0.48f,
                                    center = center
                                )
                            }
                            drawCircle(
                                color = fColor,
                                radius = cellMin * 0.34f,
                                center = center
                            )
                            if (!arenaTheme.isRetroLcd) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.7f),
                                    radius = cellMin * 0.10f,
                                    center = Offset(center.x - cellMin * 0.09f, center.y - cellMin * 0.09f)
                                )
                            }
                        }
                    }

                    if (food.expiresAtTick != null) {
                        val rem = food.remainingFraction(gameState.currentTick)
                        val arcColor = when {
                            food.type == FoodType.POISON_SKULL && !arenaTheme.isRetroLcd ->
                                if (arenaTheme.isLightBoard) Color(0xFFD90429).copy(alpha = 0.90f) else Color(0xFFFF1744).copy(alpha = 0.88f)
                            arenaTheme.isLightBoard -> Color(0xFF1F2329).copy(alpha = 0.78f)
                            else -> Color.White.copy(alpha = 0.85f)
                        }
                        drawArc(
                            color = arcColor,
                            startAngle = -90f,
                            sweepAngle = 360f * rem,
                            useCenter = false,
                            topLeft = Offset(center.x - cellMin * 0.45f, center.y - cellMin * 0.45f),
                            size = Size(cellMin * 0.90f, cellMin * 0.90f),
                            style = Stroke(width = cellMin * 0.07f, cap = StrokeCap.Round)
                        )
                    }
                }

                // 6. Draw Particle Bursts
                for (burst in gameState.recentBursts) {
                    val age = (gameState.currentTick - burst.createdAtTick).coerceAtLeast(0)
                    val progress = (age.toFloat() / 6f).coerceIn(0f, 1f)
                    val center = Offset((burst.gridX + 0.5f) * cellW, (burst.gridY + 0.5f) * cellH)
                    drawCircle(
                        color = burst.color.copy(alpha = (1f - progress) * 0.65f),
                        radius = cellMin * (0.35f + progress * 0.85f),
                        center = center,
                        style = Stroke(width = cellMin * 0.08f)
                    )
                }

                // 7. Draw Snake Body & Articulated Corner Joints
                val snake = gameState.snake
                val prevSnake = gameState.previousSnake
                val interp = if (smoothMovement && gameState.status == GameStatus.RUNNING) {
                    subTickFraction.coerceIn(0f, 1f)
                } else {
                    1f
                }
                val snakeAlpha = if (phaseGhost) 0.62f else 1.0f

                val drawCoords = Array(snake.size) { i ->
                    val currPt = snake[i]
                    val prevPt = prevSnake.getOrNull(i) ?: currPt
                    if (currPt.manhattanTo(prevPt) <= 1) {
                        Offset(
                            x = prevPt.x + (currPt.x - prevPt.x) * interp,
                            y = prevPt.y + (currPt.y - prevPt.y) * interp
                        )
                    } else {
                        Offset(currPt.x.toFloat(), currPt.y.toFloat())
                    }
                }

                // Draw active Flux Magnet field around snake head
                if (gameState.isMagnetActive && drawCoords.isNotEmpty()) {
                    val headPos = drawCoords[0]
                    val headCenter = Offset((headPos.x + 0.5f) * cellW, (headPos.y + 0.5f) * cellH)
                    val magnetColor = when {
                        arenaTheme.isRetroLcd -> Color(0xFF0F380F)
                        arenaTheme.isLightBoard -> Color(0xFF0284C7)
                        else -> Color(0xFF38BDF8)
                    }
                    drawCircle(
                        color = magnetColor.copy(alpha = if (arenaTheme.isLightBoard) 0.28f else 0.22f),
                        radius = cellMin * 3.8f,
                        center = headCenter,
                        style = Stroke(
                            width = (cellMin * 0.06f).coerceAtLeast(1.5f),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(cellMin * 0.35f, cellMin * 0.25f))
                        )
                    )
                    drawCircle(
                        color = magnetColor.copy(alpha = 0.50f),
                        radius = cellMin * 0.74f,
                        center = headCenter,
                        style = Stroke(width = (cellMin * 0.07f).coerceAtLeast(1.5f))
                    )
                }

                if (snake.size >= 2) {
                    val inset = 0.10f
                    val corner = if (arenaTheme.isRetroLcd) cellMin * 0.08f else cellMin * 0.28f
                    for (i in (snake.size - 2) downTo 0) {
                        val currA = snake[i]
                        val currB = snake[i + 1]
                        if (currA.manhattanTo(currB) == 1) {
                            val c1 = drawCoords[i]
                            val c2 = drawCoords[i + 1]
                            val pivot = Offset(currB.x.toFloat(), currB.y.toFloat())
                            val t = (i + 0.5f) / (snake.size - 1).coerceAtLeast(1).toFloat()
                            val jointColor = if (arenaTheme.isRetroLcd) {
                                Color(0xFF0F380F)
                            } else {
                                lerp(skin.bodyStartColor, skin.bodyEndColor, t)
                            }.copy(alpha = snakeAlpha)

                            // Sub-joint A: from interpolated segment i to grid pivot (currB)
                            if (abs(c1.x - pivot.x) + abs(c1.y - pivot.y) <= 1.05f) {
                                val minXA = min(c1.x, pivot.x) + inset
                                val minYA = min(c1.y, pivot.y) + inset
                                val maxXA = max(c1.x, pivot.x) + 1f - inset
                                val maxYA = max(c1.y, pivot.y) + 1f - inset
                                drawRoundRect(
                                    color = jointColor,
                                    topLeft = Offset(minXA * cellW, minYA * cellH),
                                    size = Size((maxXA - minXA) * cellW, (maxYA - minYA) * cellH),
                                    cornerRadius = CornerRadius(corner, corner)
                                )
                            }

                            // Sub-joint B: from grid pivot (currB) to interpolated segment i+1
                            if (abs(c2.x - pivot.x) + abs(c2.y - pivot.y) <= 1.05f) {
                                val minXB = min(c2.x, pivot.x) + inset
                                val minYB = min(c2.y, pivot.y) + inset
                                val maxXB = max(c2.x, pivot.x) + 1f - inset
                                val maxYB = max(c2.y, pivot.y) + 1f - inset
                                drawRoundRect(
                                    color = jointColor,
                                    topLeft = Offset(minXB * cellW, minYB * cellH),
                                    size = Size((maxXB - minXB) * cellW, (maxYB - minYB) * cellH),
                                    cornerRadius = CornerRadius(corner, corner)
                                )
                            }
                        }
                    }
                }

                for (i in (snake.size - 1) downTo 0) {
                    val drawPos = drawCoords[i]
                    val isHead = (i == 0)
                    val isTail = (i == snake.size - 1 && snake.size > 2)
                    val t = if (snake.size <= 1) 0f else i.toFloat() / (snake.size - 1).toFloat()

                    val segColor = when {
                        arenaTheme.isRetroLcd -> Color(0xFF0F380F)
                        isHead -> skin.headColor
                        else -> lerp(skin.bodyStartColor, skin.bodyEndColor, t)
                    }.copy(alpha = snakeAlpha)

                    val insetRatio = when {
                        isHead -> 0.04f
                        isTail -> 0.15f
                        else -> 0.09f
                    }
                    val segLeft = (drawPos.x + insetRatio) * cellW
                    val segTop = (drawPos.y + insetRatio) * cellH
                    val segW = cellW * (1f - insetRatio * 2f)
                    val segH = cellH * (1f - insetRatio * 2f)
                    val corner = if (arenaTheme.isRetroLcd) cellMin * 0.08f else cellMin * 0.32f

                    if (isHead && !arenaTheme.isRetroLcd) {
                        val glowAlpha = if (arenaTheme.isLightBoard) 0.14f else 0.25f
                        drawCircle(
                            color = skin.glowColor.copy(alpha = glowAlpha * snakeAlpha),
                            radius = cellMin * 0.58f,
                            center = Offset((drawPos.x + 0.5f) * cellW, (drawPos.y + 0.5f) * cellH)
                        )
                    }

                    drawRoundRect(
                        color = segColor,
                        topLeft = Offset(segLeft, segTop),
                        size = Size(segW, segH),
                        cornerRadius = CornerRadius(corner, corner)
                    )

                    if (isHead) {
                        drawSerpentEyes(
                            headX = drawPos.x,
                            headY = drawPos.y,
                            cellW = cellW,
                            cellH = cellH,
                            cellMin = cellMin,
                            direction = gameState.direction,
                            eyeColor = if (arenaTheme.isRetroLcd) arenaTheme.boardBgStart else skin.eyeColor
                        )
                    }
                }

                // 8. Draw Outer Arena Containment / Wrap Border
                val borderStroke = (cellMin * 0.11f).coerceAtLeast(3f)
                val borderEffect = if (gameState.mode.wrapsWalls) {
                    PathEffect.dashPathEffect(floatArrayOf(cellMin * 0.45f, cellMin * 0.28f), 0f)
                } else {
                    null
                }
                drawRoundRect(
                    color = arenaTheme.wallColor,
                    cornerRadius = CornerRadius(cellMin * 0.45f, cellMin * 0.45f),
                    size = size,
                    style = Stroke(width = borderStroke, pathEffect = borderEffect)
                )
            }
        }
    }
}

private fun lightBoardFoodColor(type: FoodType): Color = when (type) {
    FoodType.CYBER_APPLE -> Color(0xFFD91E48)
    FoodType.GOLDEN_CORE -> Color(0xFFD97706)
    FoodType.CHRONO_SHARD -> Color(0xFF0284C7)
    FoodType.PHASE_GHOST -> Color(0xFF7C3AED)
    FoodType.SCORE_OVERDRIVE -> Color(0xFF059669)
    FoodType.MAGNET_CORE -> Color(0xFF0284C7)
    FoodType.SHRINK_CAPSULE -> Color(0xFFEA580C)
    FoodType.POISON_SKULL -> Color(0xFF23262B)
}

private fun DrawScope.drawPoisonSkull(
    center: Offset,
    cellMin: Float,
    isRetroLcd: Boolean,
    isLightBoard: Boolean,
    boardBg: Color
) {
    val skullColor = when {
        isRetroLcd -> Color(0xFF0F380F)
        isLightBoard -> Color(0xFF23262B)
        else -> Color(0xFFD9F99D)
    }
    val hazardGlow = when {
        isRetroLcd -> Color(0xFF306230)
        isLightBoard -> Color(0xFFD90429)
        else -> Color(0xFFFF1744)
    }
    val socketColor = when {
        isRetroLcd -> boardBg
        isLightBoard -> Color(0xFFF2EFE9)
        else -> Color(0xFF1F0910)
    }

    if (!isRetroLcd) {
        // Danger aura behind skull
        drawCircle(
            color = hazardGlow.copy(alpha = if (isLightBoard) 0.16f else 0.26f),
            radius = cellMin * 0.48f,
            center = center
        )
    }

    // Crossbones / Hazard X behind cranium
    val boneSpan = cellMin * 0.34f
    val boneStroke = (cellMin * 0.08f).coerceAtLeast(1.5f)
    drawLine(
        color = if (isRetroLcd) skullColor else hazardGlow,
        start = Offset(center.x - boneSpan, center.y - boneSpan),
        end = Offset(center.x + boneSpan, center.y + boneSpan),
        strokeWidth = boneStroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = if (isRetroLcd) skullColor else hazardGlow,
        start = Offset(center.x + boneSpan, center.y - boneSpan),
        end = Offset(center.x - boneSpan, center.y + boneSpan),
        strokeWidth = boneStroke,
        cap = StrokeCap.Round
    )

    // Upper Skull Cranium
    val craniumRadius = cellMin * 0.27f
    val craniumCenter = Offset(center.x, center.y - cellMin * 0.05f)
    drawCircle(
        color = skullColor,
        radius = craniumRadius,
        center = craniumCenter
    )

    // Lower Jaw Block
    val jawW = cellMin * 0.30f
    val jawH = cellMin * 0.20f
    drawRoundRect(
        color = skullColor,
        topLeft = Offset(center.x - jawW / 2f, center.y + cellMin * 0.06f),
        size = Size(jawW, jawH),
        cornerRadius = CornerRadius(cellMin * 0.06f, cellMin * 0.06f)
    )

    // Eye Sockets (left & right)
    val eyeOffsetX = cellMin * 0.10f
    val eyeY = center.y - cellMin * 0.06f
    val eyeR = (cellMin * 0.065f).coerceAtLeast(1.5f)
    drawCircle(color = socketColor, radius = eyeR, center = Offset(center.x - eyeOffsetX, eyeY))
    drawCircle(color = socketColor, radius = eyeR, center = Offset(center.x + eyeOffsetX, eyeY))

    if (!isRetroLcd) {
        // Glowing red pupils inside skull sockets
        drawCircle(
            color = hazardGlow,
            radius = eyeR * 0.48f,
            center = Offset(center.x - eyeOffsetX, eyeY)
        )
        drawCircle(
            color = hazardGlow,
            radius = eyeR * 0.48f,
            center = Offset(center.x + eyeOffsetX, eyeY)
        )
    }

    // Jaw Teeth lines
    val toothStroke = (cellMin * 0.035f).coerceAtLeast(1f)
    for (dxFactor in listOf(-0.06f, 0f, 0.06f)) {
        val tx = center.x + cellMin * dxFactor
        drawLine(
            color = socketColor,
            start = Offset(tx, center.y + cellMin * 0.12f),
            end = Offset(tx, center.y + cellMin * 0.24f),
            strokeWidth = toothStroke
        )
    }
}

private fun DrawScope.drawMagnetOrb(
    center: Offset,
    cellMin: Float,
    isRetroLcd: Boolean,
    isLightBoard: Boolean
) {
    val fieldColor = when {
        isRetroLcd -> Color(0xFF0F380F)
        isLightBoard -> Color(0xFF0284C7)
        else -> Color(0xFF38BDF8)
    }
    val archColor = when {
        isRetroLcd -> Color(0xFF0F380F)
        isLightBoard -> Color(0xFF0369A1)
        else -> Color(0xFF0284C7)
    }
    val poleRed = when {
        isRetroLcd -> Color(0xFF0F380F)
        isLightBoard -> Color(0xFFD91E48)
        else -> Color(0xFFFF2A6D)
    }
    val poleTip = if (isLightBoard) Color(0xFF334155) else Color.White

    if (!isRetroLcd) {
        drawCircle(
            color = fieldColor.copy(alpha = if (isLightBoard) 0.16f else 0.26f),
            radius = cellMin * 0.48f,
            center = center
        )
    }

    // Horseshoe U-Magnet Arch (opening facing up)
    val uRadius = cellMin * 0.22f
    val strokeW = (cellMin * 0.11f).coerceAtLeast(2f)
    val archCenterY = center.y + cellMin * 0.02f

    // Bottom semicircle of U
    drawArc(
        color = archColor,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - uRadius, archCenterY - uRadius),
        size = Size(uRadius * 2f, uRadius * 2f),
        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
    )

    // Left & Right vertical arms of U
    val armTopY = center.y - cellMin * 0.20f
    drawLine(
        color = poleRed,
        start = Offset(center.x - uRadius, armTopY),
        end = Offset(center.x - uRadius, archCenterY),
        strokeWidth = strokeW,
        cap = StrokeCap.Butt
    )
    drawLine(
        color = fieldColor,
        start = Offset(center.x + uRadius, armTopY),
        end = Offset(center.x + uRadius, archCenterY),
        strokeWidth = strokeW,
        cap = StrokeCap.Butt
    )

    // Pole tips at the top of the arms
    if (!isRetroLcd) {
        val tipHeight = cellMin * 0.07f
        drawLine(
            color = poleTip,
            start = Offset(center.x - uRadius, armTopY),
            end = Offset(center.x - uRadius, armTopY + tipHeight),
            strokeWidth = strokeW,
            cap = StrokeCap.Square
        )
        drawLine(
            color = poleTip,
            start = Offset(center.x + uRadius, armTopY),
            end = Offset(center.x + uRadius, armTopY + tipHeight),
            strokeWidth = strokeW,
            cap = StrokeCap.Square
        )
    }
}

private fun DrawScope.drawPortalNode(center: Offset, cellMin: Float, color: Color) {
    drawCircle(
        color = color.copy(alpha = 0.22f),
        radius = cellMin * 0.46f,
        center = center
    )
    drawCircle(
        color = color,
        radius = cellMin * 0.36f,
        center = center,
        style = Stroke(width = cellMin * 0.09f)
    )
    drawCircle(
        color = Color.White,
        radius = cellMin * 0.14f,
        center = center
    )
}

private fun DrawScope.drawSerpentEyes(
    headX: Float,
    headY: Float,
    cellW: Float,
    cellH: Float,
    cellMin: Float,
    direction: Direction,
    eyeColor: Color
) {
    val cx = (headX + 0.5f) * cellW
    val cy = (headY + 0.5f) * cellH
    val fwd = cellMin * 0.16f
    val side = cellMin * 0.18f
    val eyeRadius = (cellMin * 0.09f).coerceAtLeast(2f)

    val (eye1, eye2) = when (direction) {
        Direction.RIGHT -> Offset(cx + fwd, cy - side) to Offset(cx + fwd, cy + side)
        Direction.LEFT -> Offset(cx - fwd, cy - side) to Offset(cx - fwd, cy + side)
        Direction.UP -> Offset(cx - side, cy - fwd) to Offset(cx + side, cy - fwd)
        Direction.DOWN -> Offset(cx - side, cy + fwd) to Offset(cx + side, cy + fwd)
    }

    drawCircle(color = eyeColor, radius = eyeRadius, center = eye1)
    drawCircle(color = eyeColor, radius = eyeRadius, center = eye2)
}
