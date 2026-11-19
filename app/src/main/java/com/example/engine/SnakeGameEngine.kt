package com.example.engine

import com.example.model.ActiveBuff
import com.example.model.DeathReason
import com.example.model.Direction
import com.example.model.FoodType
import com.example.model.GameSettings
import com.example.model.GameState
import com.example.model.GameStatus
import com.example.model.GridPoint
import com.example.model.LabyrinthLayout
import com.example.model.ParticleBurst
import com.example.model.PortalPair
import com.example.model.SpawnedFood
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

sealed interface GameEvent {
    data class AteFood(val foodType: FoodType, val pointsEarned: Int, val newCombo: Int) : GameEvent
    data object BonusFoodSpawned : GameEvent
    data class PortalWarped(val bonusPoints: Int) : GameEvent
    data object Turned : GameEvent
    data object BlitzWallPenalty : GameEvent
    data class GameEnded(val status: GameStatus, val reason: DeathReason) : GameEvent
}

data class EngineStepResult(
    val state: GameState,
    val events: List<GameEvent>
)

class SnakeGameEngine(
    private val random: Random = Random.Default
) {
    private var nextEntityId = 1L

    fun createNewGame(
        settings: GameSettings,
        highScoreForMode: Int = 0,
        overrideWidth: Int? = null,
        overrideHeight: Int? = null
    ): GameState {
        val width = (overrideWidth ?: settings.boardSize.columns).coerceIn(10, 48)
        val height = (overrideHeight ?: settings.boardSize.rows).coerceIn(10, 48)
        val centerX = width / 2
        val centerY = height / 2

        val initialSnake = listOf(
            GridPoint(centerX, centerY),
            GridPoint(centerX - 1, centerY),
            GridPoint(centerX - 2, centerY)
        )

        val safeSpawnZone = buildSet {
            for (dx in -4..5) {
                for (dy in -1..1) {
                    add(GridPoint(centerX + dx, centerY + dy))
                }
            }
        }

        val obstacles = if (settings.gameMode.hasObstacles) {
            generateObstacles(width, height, settings.labyrinthLayout, safeSpawnZone)
        } else {
            emptySet()
        }

        val portals = if (settings.gameMode.hasPortals) {
            generatePortals(width, height, obstacles, safeSpawnZone)
        } else {
            emptyList()
        }

        val baseState = GameState(
            mode = settings.gameMode,
            difficulty = settings.difficulty,
            boardWidth = width,
            boardHeight = height,
            labyrinthLayout = settings.labyrinthLayout,
            snake = initialSnake,
            previousSnake = initialSnake,
            direction = Direction.RIGHT,
            queuedDirections = emptyList(),
            obstacles = obstacles,
            portals = portals,
            foods = emptyList(),
            activeBuffs = emptyList(),
            recentBursts = emptyList(),
            lastPoisonPosition = null,
            nextPoisonSpawnTick = 0,
            score = 0,
            initialHighScoreForMode = highScoreForMode,
            highScoreForMode = highScoreForMode,
            applesEaten = 0,
            goldenEaten = 0,
            powerUpsCollected = 0,
            portalsUsed = 0,
            comboCount = 1,
            maxComboReached = 1,
            ticksSinceLastEat = 0,
            comboWindowTicks = max(width, height) + 2,
            currentTick = 0,
            currentTickIntervalMs = settings.difficulty.baseTickMs,
            remainingBlitzMillis = 120_000L,
            elapsedPlayMillis = 0L,
            pendingGrowth = 0,
            status = GameStatus.READY,
            deathReason = DeathReason.NONE,
            isNewHighScore = false
        )

        val initialFoods = mutableListOf<SpawnedFood>()
        spawnFoodOfType(baseState, FoodType.CYBER_APPLE)?.let { initialFoods.add(it) }
        val stateWithApple = baseState.copy(foods = initialFoods)

        if (settings.poisonEnabled) {
            spawnPoisonSkull(stateWithApple)?.let { initialFoods.add(it) }
        }

        return stateWithApple.copy(foods = initialFoods.toList())
    }

    fun resizeBoardPreservingState(
        state: GameState,
        settings: GameSettings,
        newWidth: Int,
        newHeight: Int
    ): GameState {
        val targetW = newWidth.coerceIn(10, 48)
        val targetH = newHeight.coerceIn(10, 48)
        if (state.boardWidth == targetW && state.boardHeight == targetH) {
            return state
        }

        if (state.status == GameStatus.READY) {
            return createNewGame(
                settings = settings,
                highScoreForMode = state.highScoreForMode,
                overrideWidth = targetW,
                overrideHeight = targetH
            )
        }

        // Preserve active/paused run by shifting snake contiguously inside bounds and regenerating out-of-bounds items safely
        val maxX = state.snake.maxOfOrNull { it.x } ?: 0
        val maxY = state.snake.maxOfOrNull { it.y } ?: 0
        val shiftX = if (maxX >= targetW) (targetW - 1 - maxX) else 0
        val shiftY = if (maxY >= targetH) (targetH - 1 - maxY) else 0
        val adjustedSnake = state.snake.map { pt ->
            GridPoint(
                x = (pt.x + shiftX).coerceIn(0, targetW - 1),
                y = (pt.y + shiftY).coerceIn(0, targetH - 1)
            )
        }
        val adjustedPrevSnake = state.previousSnake.map { pt ->
            GridPoint(
                x = (pt.x + shiftX).coerceIn(0, targetW - 1),
                y = (pt.y + shiftY).coerceIn(0, targetH - 1)
            )
        }
        val head = adjustedSnake.firstOrNull() ?: GridPoint(targetW / 2, targetH / 2)
        val safeZone = buildSet {
            addAll(adjustedSnake)
            for (step in 1..3) {
                add(GridPoint(head.x + state.direction.dx * step, head.y + state.direction.dy * step).wrap(targetW, targetH))
            }
        }

        val obstacles = if (state.mode.hasObstacles) {
            generateObstacles(targetW, targetH, state.labyrinthLayout, safeZone)
        } else {
            emptySet()
        }

        val portals = if (state.mode.hasPortals) {
            generatePortals(targetW, targetH, obstacles, safeZone)
        } else {
            emptyList()
        }

        val validFoods = state.foods.filter { f ->
            f.position.isInside(targetW, targetH) &&
                f.position !in adjustedSnake &&
                f.position !in obstacles &&
                portals.none { it.entryA == f.position || it.entryB == f.position } &&
                (f.type != FoodType.POISON_SKULL || (settings.poisonEnabled && f.position.manhattanTo(head) >= MIN_POISON_HEAD_MANHATTAN))
        }.toMutableList()

        val resizedBase = state.copy(
            boardWidth = targetW,
            boardHeight = targetH,
            snake = adjustedSnake,
            previousSnake = adjustedPrevSnake,
            obstacles = obstacles,
            portals = portals,
            foods = validFoods,
            comboWindowTicks = max(targetW, targetH) + 2
        )

        if (validFoods.none { it.type == FoodType.CYBER_APPLE }) {
            spawnFoodOfType(resizedBase, FoodType.CYBER_APPLE)?.let { validFoods.add(it) }
        }
        if (settings.poisonEnabled && validFoods.none { it.type == FoodType.POISON_SKULL }) {
            spawnPoisonSkull(resizedBase.copy(foods = validFoods))?.let { validFoods.add(it) }
        }

        return resizedBase.copy(foods = validFoods)
    }

    fun enqueueDirection(state: GameState, requested: Direction): Pair<GameState, Boolean> {
        if (state.status != GameStatus.RUNNING && state.status != GameStatus.READY) {
            return state to false
        }
        if (state.status == GameStatus.READY && state.queuedDirections.isEmpty() && requested == state.direction) {
            return state.copy(status = GameStatus.RUNNING) to true
        }
        // Restrict buffered turn queue to at most 2 moves so rapid L-swipes never queue a 3rd self-biting turn
        if (state.queuedDirections.size >= 2) {
            return state to false
        }
        val referenceDir = state.queuedDirections.lastOrNull() ?: state.direction
        if (requested == referenceDir || requested.isOpposite(referenceDir)) {
            return state to false
        }
        val updatedQueue = state.queuedDirections + requested
        val updatedStatus = if (state.status == GameStatus.READY) GameStatus.RUNNING else state.status
        return state.copy(
            queuedDirections = updatedQueue,
            status = updatedStatus
        ) to true
    }

    fun enqueueRelativeTurn(state: GameState, clockwise: Boolean): Pair<GameState, Boolean> {
        val referenceDir = state.queuedDirections.lastOrNull() ?: state.direction
        val targetDir = if (clockwise) referenceDir.turnRight() else referenceDir.turnLeft()
        return enqueueDirection(state, targetDir)
    }

    fun stepTick(
        state: GameState,
        settings: GameSettings
    ): EngineStepResult {
        if (state.status != GameStatus.RUNNING) {
            return EngineStepResult(state, emptyList())
        }

        val events = mutableListOf<GameEvent>()
        val nextTick = state.currentTick + 1
        val tickDurationMs = state.currentTickIntervalMs
        val newElapsedMillis = state.elapsedPlayMillis + tickDurationMs

        var updatedBlitzMillis = state.remainingBlitzMillis
        if (state.mode.isTimedBlitz) {
            updatedBlitzMillis = (state.remainingBlitzMillis - tickDurationMs).coerceAtLeast(0L)
            if (updatedBlitzMillis <= 0L) {
                val endedState = state.copy(
                    remainingBlitzMillis = 0L,
                    elapsedPlayMillis = newElapsedMillis,
                    status = GameStatus.TIME_UP,
                    deathReason = DeathReason.TIME_EXPIRED
                )
                events.add(GameEvent.GameEnded(GameStatus.TIME_UP, DeathReason.TIME_EXPIRED))
                return EngineStepResult(endedState, events)
            }
        }

        val activeDirection: Direction
        val remainingQueue: List<Direction>
        if (state.queuedDirections.isNotEmpty()) {
            val candidate = state.queuedDirections.first()
            remainingQueue = state.queuedDirections.drop(1)
            activeDirection = if (!candidate.isOpposite(state.direction)) {
                if (candidate != state.direction) {
                    events.add(GameEvent.Turned)
                }
                candidate
            } else {
                state.direction
            }
        } else {
            activeDirection = state.direction
            remainingQueue = emptyList()
        }

        val currentHead = state.snake.first()
        var candidateHead = currentHead.step(activeDirection)
        var currentSnake = state.snake
        var currentCombo = state.comboCount
        var ticksSinceEat = state.ticksSinceLastEat + 1
        if (ticksSinceEat > state.comboWindowTicks && currentCombo > 1) {
            currentCombo = 1
        }

        val wraps = state.mode.wrapsWalls
        if (!candidateHead.isInside(state.boardWidth, state.boardHeight)) {
            if (wraps) {
                candidateHead = candidateHead.wrap(state.boardWidth, state.boardHeight)
            } else if (state.mode.isTimedBlitz) {
                candidateHead = candidateHead.wrap(state.boardWidth, state.boardHeight)
                if (currentSnake.size > 3) {
                    val trimCount = minOf(2, currentSnake.size - 3)
                    currentSnake = currentSnake.dropLast(trimCount)
                }
                currentCombo = 1
                events.add(GameEvent.BlitzWallPenalty)
            } else {
                val ended = state.copy(
                    direction = activeDirection,
                    queuedDirections = remainingQueue,
                    elapsedPlayMillis = newElapsedMillis,
                    status = GameStatus.GAME_OVER,
                    deathReason = DeathReason.WALL_COLLISION
                )
                events.add(GameEvent.GameEnded(GameStatus.GAME_OVER, DeathReason.WALL_COLLISION))
                return EngineStepResult(ended, events)
            }
        }

        var portalsUsedCount = state.portalsUsed
        var portalBonusPoints = 0
        val newBursts = state.recentBursts.filter { nextTick - it.createdAtTick <= 6 }.toMutableList()
        for (portal in state.portals) {
            val matchedExit = when (candidateHead) {
                portal.entryA -> portal.entryB
                portal.entryB -> portal.entryA
                else -> null
            }
            if (matchedExit != null) {
                candidateHead = matchedExit
                portalsUsedCount++
                val overdriveMult = if (state.isOverdriveActive) 2.0f else 1.0f
                portalBonusPoints = (15f * state.difficulty.scoreMultiplier * overdriveMult).roundToInt().coerceAtLeast(10)
                events.add(GameEvent.PortalWarped(portalBonusPoints))
                newBursts.add(
                    ParticleBurst(
                        id = nextEntityId++,
                        gridX = matchedExit.x.toFloat(),
                        gridY = matchedExit.y.toFloat(),
                        color = androidx.compose.ui.graphics.Color(portal.colorHex),
                        createdAtTick = nextTick
                    )
                )
                break
            }
        }

        val phaseGhostActive = state.isPhaseGhostActive
        if (!phaseGhostActive && candidateHead in state.obstacles) {
            val ended = state.copy(
                direction = activeDirection,
                queuedDirections = remainingQueue,
                elapsedPlayMillis = newElapsedMillis,
                status = GameStatus.GAME_OVER,
                deathReason = DeathReason.OBSTACLE_COLLISION
            )
            events.add(GameEvent.GameEnded(GameStatus.GAME_OVER, DeathReason.OBSTACLE_COLLISION))
            return EngineStepResult(ended, events)
        }

        // Check if snake stepped onto a Poison Skull hazard
        val steppedOnPoison = state.foods.firstOrNull {
            it.position == candidateHead && it.type == FoodType.POISON_SKULL
        }
        if (steppedOnPoison != null && !phaseGhostActive) {
            val ended = state.copy(
                snake = listOf(candidateHead) + currentSnake.dropLast(1),
                previousSnake = currentSnake,
                direction = activeDirection,
                queuedDirections = remainingQueue,
                elapsedPlayMillis = newElapsedMillis,
                status = GameStatus.GAME_OVER,
                deathReason = DeathReason.POISON_CONSUMED
            )
            events.add(GameEvent.GameEnded(GameStatus.GAME_OVER, DeathReason.POISON_CONSUMED))
            return EngineStepResult(ended, events)
        }

        // Apply Flux Magnet attraction (pulls non-poison orbs within 4 tiles 1 step closer to candidateHead)
        val magnetActiveNow = state.isMagnetActive ||
            state.foods.any { it.position == candidateHead && it.type == FoodType.MAGNET_CORE }

        val foodsAfterMagnet = if (magnetActiveNow) {
            pullFoodsTowardHead(
                foods = state.foods,
                head = candidateHead,
                snakeBody = currentSnake,
                obstacles = state.obstacles,
                portals = state.portals,
                boardWidth = state.boardWidth,
                boardHeight = state.boardHeight
            )
        } else {
            state.foods
        }

        val eatenFood = foodsAfterMagnet.firstOrNull {
            it.position == candidateHead && !it.type.isHazard
        }
        val growthDelta = eatenFood?.type?.growthAmount ?: 0
        val totalPendingGrowth = if (growthDelta < 0) 0 else state.pendingGrowth + growthDelta
        val willGrowThisStep = totalPendingGrowth > 0

        val bodyToCheck = if (willGrowThisStep) {
            currentSnake
        } else {
            currentSnake.dropLast(1)
        }

        if (!phaseGhostActive && candidateHead in bodyToCheck) {
            val ended = state.copy(
                direction = activeDirection,
                queuedDirections = remainingQueue,
                elapsedPlayMillis = newElapsedMillis,
                status = GameStatus.GAME_OVER,
                deathReason = DeathReason.SELF_COLLISION
            )
            events.add(GameEvent.GameEnded(GameStatus.GAME_OVER, DeathReason.SELF_COLLISION))
            return EngineStepResult(ended, events)
        }

        val nextPendingGrowth: Int
        val updatedSnake: List<GridPoint>
        if (growthDelta < 0) {
            val combined = listOf(candidateHead) + currentSnake.dropLast(1)
            val targetSize = max(3, combined.size + growthDelta)
            updatedSnake = combined.take(targetSize)
            nextPendingGrowth = 0
        } else if (willGrowThisStep) {
            updatedSnake = listOf(candidateHead) + currentSnake
            nextPendingGrowth = totalPendingGrowth - 1
        } else {
            updatedSnake = listOf(candidateHead) + currentSnake.dropLast(1)
            nextPendingGrowth = 0
        }

        val updatedBuffs = state.activeBuffs
            .mapNotNull { buff ->
                val left = buff.remainingTicks - 1
                if (left > 0) buff.copy(remainingTicks = left) else null
            }
            .toMutableList()

        var newScore = state.score + portalBonusPoints
        var newApplesEaten = state.applesEaten
        var newGoldenEaten = state.goldenEaten
        var newPowerUps = state.powerUpsCollected
        var maxCombo = state.maxComboReached

        // Track if an existing Poison Skull expired on this tick so it can relocate after a brief cooldown
        val expiredPoison = foodsAfterMagnet.firstOrNull { food ->
            food.type == FoodType.POISON_SKULL && food.expiresAtTick != null && food.expiresAtTick <= nextTick
        }
        var updatedLastPoisonPos = state.lastPoisonPosition
        var updatedNextPoisonTick = state.nextPoisonSpawnTick
        if (expiredPoison != null) {
            updatedLastPoisonPos = expiredPoison.position
            updatedNextPoisonTick = nextTick + POISON_RESPAWN_COOLDOWN_TICKS
        }

        val remainingFoods = foodsAfterMagnet
            .filter { food ->
                food.id != eatenFood?.id &&
                    (food.expiresAtTick == null || food.expiresAtTick > nextTick) &&
                    (settings.poisonEnabled || food.type != FoodType.POISON_SKULL)
            }
            .toMutableList()

        if (eatenFood != null) {
            currentCombo = if (ticksSinceEat <= state.comboWindowTicks) {
                (currentCombo + 1).coerceAtMost(8)
            } else {
                1
            }
            ticksSinceEat = 0
            maxCombo = max(maxCombo, currentCombo)

            val overdriveMult = if (state.isOverdriveActive || eatenFood.type == FoodType.SCORE_OVERDRIVE) 2.0f else 1.0f
            val rawPoints = eatenFood.type.basePoints *
                currentCombo *
                state.mode.scoreMultiplier *
                state.difficulty.scoreMultiplier *
                overdriveMult
            val earnedPoints = max(5, rawPoints.roundToInt())
            newScore += earnedPoints

            when (eatenFood.type) {
                FoodType.CYBER_APPLE -> {
                    newApplesEaten++
                    if (state.mode.isTimedBlitz) {
                        updatedBlitzMillis = (updatedBlitzMillis + 2_000L).coerceAtMost(120_000L)
                    }
                }
                FoodType.GOLDEN_CORE -> {
                    newGoldenEaten++
                    if (state.mode.isTimedBlitz) {
                        updatedBlitzMillis = (updatedBlitzMillis + 5_000L).coerceAtMost(120_000L)
                    }
                }
                FoodType.MAGNET_CORE,
                FoodType.CHRONO_SHARD,
                FoodType.PHASE_GHOST,
                FoodType.SCORE_OVERDRIVE -> {
                    newPowerUps++
                    val duration = eatenFood.type.buffDurationTicks
                    val existingIdx = updatedBuffs.indexOfFirst { it.type == eatenFood.type }
                    if (existingIdx >= 0) {
                        updatedBuffs[existingIdx] = ActiveBuff(
                            type = eatenFood.type,
                            remainingTicks = duration,
                            totalTicks = duration
                        )
                    } else {
                        updatedBuffs.add(
                            ActiveBuff(
                                type = eatenFood.type,
                                remainingTicks = duration,
                                totalTicks = duration
                            )
                        )
                    }
                }
                FoodType.SHRINK_CAPSULE -> {
                    newPowerUps++
                }
                FoodType.POISON_SKULL -> Unit
            }

            newBursts.add(
                ParticleBurst(
                    id = nextEntityId++,
                    gridX = candidateHead.x.toFloat(),
                    gridY = candidateHead.y.toFloat(),
                    color = eatenFood.type.color,
                    createdAtTick = nextTick
                )
            )
            events.add(GameEvent.AteFood(eatenFood.type, earnedPoints, currentCombo))
        }

        val updatedHighScore = max(state.highScoreForMode, newScore)
        val achievedNewHigh = state.isNewHighScore || (newScore > state.initialHighScoreForMode && newScore > 0)

        val intermediateState = state.copy(
            snake = updatedSnake,
            previousSnake = currentSnake.take(updatedSnake.size),
            direction = activeDirection,
            queuedDirections = remainingQueue,
            foods = remainingFoods,
            activeBuffs = updatedBuffs,
            recentBursts = newBursts,
            lastPoisonPosition = updatedLastPoisonPos,
            nextPoisonSpawnTick = updatedNextPoisonTick,
            score = newScore,
            highScoreForMode = updatedHighScore,
            isNewHighScore = achievedNewHigh,
            applesEaten = newApplesEaten,
            goldenEaten = newGoldenEaten,
            powerUpsCollected = newPowerUps,
            portalsUsed = portalsUsedCount,
            comboCount = currentCombo,
            maxComboReached = maxCombo,
            ticksSinceLastEat = ticksSinceEat,
            currentTick = nextTick,
            remainingBlitzMillis = updatedBlitzMillis,
            elapsedPlayMillis = newElapsedMillis,
            pendingGrowth = nextPendingGrowth
        )

        if (remainingFoods.none { it.type == FoodType.CYBER_APPLE }) {
            val spawnedApple = spawnFoodOfType(intermediateState, FoodType.CYBER_APPLE)
            if (spawnedApple == null) {
                val victoryScore = intermediateState.score + 1000
                val victoryState = intermediateState.copy(
                    status = GameStatus.VICTORY,
                    deathReason = DeathReason.BOARD_CLEARED,
                    score = victoryScore,
                    highScoreForMode = max(intermediateState.highScoreForMode, victoryScore),
                    isNewHighScore = true
                )
                events.add(GameEvent.GameEnded(GameStatus.VICTORY, DeathReason.BOARD_CLEARED))
                return EngineStepResult(victoryState, events)
            } else {
                remainingFoods.add(spawnedApple)
            }
        }

        // Spawn bonus Golden Core or Power-Up (excluding CYBER_APPLE and POISON_SKULL from count)
        val specialFoodsCount = remainingFoods.count {
            it.type != FoodType.CYBER_APPLE && it.type != FoodType.POISON_SKULL
        }
        if (specialFoodsCount == 0 && eatenFood?.type == FoodType.CYBER_APPLE) {
            if (newApplesEaten % 5 == 0) {
                val golden = spawnFoodOfType(
                    intermediateState.copy(foods = remainingFoods),
                    FoodType.GOLDEN_CORE
                )
                if (golden != null) {
                    remainingFoods.add(golden)
                    events.add(GameEvent.BonusFoodSpawned)
                }
            } else if (newApplesEaten % 6 == 0 || random.nextFloat() < 0.25f) {
                val powerUpOptions = buildList {
                    add(FoodType.MAGNET_CORE)
                    add(FoodType.CHRONO_SHARD)
                    add(FoodType.PHASE_GHOST)
                    add(FoodType.SCORE_OVERDRIVE)
                    if (updatedSnake.size >= 10) {
                        add(FoodType.SHRINK_CAPSULE)
                    }
                }
                val chosenType = powerUpOptions[random.nextInt(powerUpOptions.size)]
                val spawnedPowerUp = spawnFoodOfType(
                    intermediateState.copy(foods = remainingFoods),
                    chosenType
                )
                if (spawnedPowerUp != null) {
                    remainingFoods.add(spawnedPowerUp)
                    events.add(GameEvent.BonusFoodSpawned)
                }
            }
        }

        // Manage single-instance Poison Skull spawning when enabled
        if (settings.poisonEnabled) {
            val poisonCount = remainingFoods.count { it.type == FoodType.POISON_SKULL }
            if (poisonCount == 0 && nextTick >= updatedNextPoisonTick) {
                val spawnedPoison = spawnPoisonSkull(
                    intermediateState.copy(
                        foods = remainingFoods,
                        lastPoisonPosition = updatedLastPoisonPos
                    )
                )
                if (spawnedPoison != null) {
                    remainingFoods.add(spawnedPoison)
                }
            }
        }

        val slowMoActive = updatedBuffs.any { it.type == FoodType.CHRONO_SHARD && it.remainingTicks > 0 }
        val baseInterval = if (settings.dynamicSpeedScaling) {
            val extraSegments = (updatedSnake.size - 3).coerceAtLeast(0)
            val reduction = (extraSegments / 2) * state.difficulty.speedStepMs
            (state.difficulty.baseTickMs - reduction).coerceAtLeast(state.difficulty.minTickMs)
        } else {
            state.difficulty.baseTickMs
        }
        val effectiveInterval = if (slowMoActive) {
            (baseInterval * 1.45f).roundToInt().toLong()
        } else {
            baseInterval
        }

        return EngineStepResult(
            state = intermediateState.copy(
                foods = remainingFoods,
                currentTickIntervalMs = effectiveInterval
            ),
            events = events
        )
    }

    private fun pullFoodsTowardHead(
        foods: List<SpawnedFood>,
        head: GridPoint,
        snakeBody: List<GridPoint>,
        obstacles: Set<GridPoint>,
        portals: List<PortalPair>,
        boardWidth: Int,
        boardHeight: Int
    ): List<SpawnedFood> {
        if (foods.isEmpty()) return foods

        val occupiedPositions = foods.mapTo(HashSet()) { it.position }
        val portalPositions = HashSet<GridPoint>(portals.size * 2).apply {
            portals.forEach {
                add(it.entryA)
                add(it.entryB)
            }
        }
        val bodySet = snakeBody.toHashSet()

        return foods.map { food ->
            // CRITICAL: Never pull Poison Skull or any hazard under any circumstances
            if (food.type == FoodType.POISON_SKULL || food.type.isHazard) {
                return@map food
            }
            val pos = food.position
            if (pos == head) {
                return@map food
            }

            val chebyshevDist = pos.chebyshevTo(head)
            if (chebyshevDist !in 1..MAGNET_PULL_RADIUS) {
                return@map food
            }

            val dx = head.x - pos.x
            val dy = head.y - pos.y
            val stepX = if (dx != 0) GridPoint(pos.x + dx.coerceIn(-1, 1), pos.y) else null
            val stepY = if (dy != 0) GridPoint(pos.x, pos.y + dy.coerceIn(-1, 1)) else null

            val candidates = if (abs(dx) >= abs(dy)) {
                listOfNotNull(stepX, stepY)
            } else {
                listOfNotNull(stepY, stepX)
            }

            val chosenStep = candidates.firstOrNull { candidate ->
                candidate.isInside(boardWidth, boardHeight) &&
                    candidate !in obstacles &&
                    candidate !in portalPositions &&
                    candidate !in occupiedPositions &&
                    (candidate == head || candidate !in bodySet)
            }

            if (chosenStep != null) {
                occupiedPositions.remove(pos)
                occupiedPositions.add(chosenStep)
                food.copy(position = chosenStep)
            } else {
                food
            }
        }
    }

    fun spawnPoisonSkull(state: GameState): SpawnedFood? {
        // Enforce at most 1 Poison Skull on the grid at any time
        if (state.foods.any { it.type == FoodType.POISON_SKULL }) {
            return null
        }

        val head = state.snake.firstOrNull() ?: GridPoint(state.boardWidth / 2, state.boardHeight / 2)
        val occupied = HashSet<GridPoint>(
            state.snake.size + state.obstacles.size + state.foods.size + state.portals.size * 2 + 8
        )
        occupied.addAll(state.snake)
        occupied.addAll(state.obstacles)
        state.foods.forEach { occupied.add(it.position) }
        state.portals.forEach {
            occupied.add(it.entryA)
            occupied.add(it.entryB)
        }
        // Exclude previous poison location so it always reappears at a different tile
        state.lastPoisonPosition?.let { occupied.add(it) }

        // Exclude immediate forward line of travel in front of the snake head
        for (step in 1..5) {
            occupied.add(
                GridPoint(
                    head.x + state.direction.dx * step,
                    head.y + state.direction.dy * step
                ).wrap(state.boardWidth, state.boardHeight)
            )
        }

        val safeCandidates = ArrayList<GridPoint>(state.boardWidth * state.boardHeight)
        for (x in 0 until state.boardWidth) {
            for (y in 0 until state.boardHeight) {
                val pt = GridPoint(x, y)
                if (pt !in occupied &&
                    pt.manhattanTo(head) >= MIN_POISON_HEAD_MANHATTAN &&
                    pt.chebyshevTo(head) >= MIN_POISON_HEAD_CHEBYSHEV
                ) {
                    safeCandidates.add(pt)
                }
            }
        }

        if (safeCandidates.isEmpty()) return null
        val chosenPoint = safeCandidates[random.nextInt(safeCandidates.size)]
        val expiresAt = FoodType.POISON_SKULL.lifespanTicks?.let { state.currentTick + it }
        return SpawnedFood(
            id = nextEntityId++,
            position = chosenPoint,
            type = FoodType.POISON_SKULL,
            spawnedAtTick = state.currentTick,
            expiresAtTick = expiresAt
        )
    }

    fun spawnFoodOfType(state: GameState, type: FoodType): SpawnedFood? {
        if (type == FoodType.POISON_SKULL) {
            return spawnPoisonSkull(state)
        }

        val occupied = HashSet<GridPoint>(
            state.snake.size + state.obstacles.size + state.foods.size + state.portals.size * 2
        )
        occupied.addAll(state.snake)
        occupied.addAll(state.obstacles)
        state.foods.forEach { occupied.add(it.position) }
        state.portals.forEach {
            occupied.add(it.entryA)
            occupied.add(it.entryB)
        }

        val available = ArrayList<GridPoint>(state.boardWidth * state.boardHeight)
        for (x in 0 until state.boardWidth) {
            for (y in 0 until state.boardHeight) {
                val pt = GridPoint(x, y)
                if (pt !in occupied) {
                    available.add(pt)
                }
            }
        }

        if (available.isEmpty()) return null
        val chosenPoint = available[random.nextInt(available.size)]
        val expiresAt = type.lifespanTicks?.let { state.currentTick + it }
        return SpawnedFood(
            id = nextEntityId++,
            position = chosenPoint,
            type = type,
            spawnedAtTick = state.currentTick,
            expiresAtTick = expiresAt
        )
    }

    companion object {
        const val MAGNET_PULL_RADIUS = 4
        const val MIN_POISON_HEAD_MANHATTAN = 5
        const val MIN_POISON_HEAD_CHEBYSHEV = 4
        const val POISON_RESPAWN_COOLDOWN_TICKS = 5

        fun computeFullAreaDimensions(
            baseBoardSize: com.example.model.BoardSize,
            areaWidthPx: Int,
            areaHeightPx: Int
        ): Pair<Int, Int> {
            val baseDim = baseBoardSize.columns
            if (areaWidthPx <= 0 || areaHeightPx <= 0) {
                return baseDim to (baseDim * 1.3f).roundToInt().coerceIn(12, 46)
            }
            return if (areaHeightPx >= areaWidthPx) {
                val cols = baseDim
                val rows = ((baseDim.toFloat() * areaHeightPx.toFloat()) / areaWidthPx.toFloat())
                    .roundToInt()
                    .coerceIn(10, 48)
                cols to rows
            } else {
                val rows = baseDim
                val cols = ((baseDim.toFloat() * areaWidthPx.toFloat()) / areaHeightPx.toFloat())
                    .roundToInt()
                    .coerceIn(10, 48)
                cols to rows
            }
        }

        fun generateObstacles(
            width: Int,
            height: Int,
            layout: LabyrinthLayout,
            safeZone: Set<GridPoint>
        ): Set<GridPoint> {
            val raw = mutableSetOf<GridPoint>()
            val midX = width / 2
            val midY = height / 2

            when (layout) {
                LabyrinthLayout.CROSSHAIR_PILLARS -> {
                    val offsetX = max(3, width / 4)
                    val offsetY = max(3, height / 4)
                    for (dx in -1..1) {
                        raw.add(GridPoint(offsetX + dx, offsetY))
                        raw.add(GridPoint(width - 1 - offsetX + dx, offsetY))
                        raw.add(GridPoint(offsetX + dx, height - 1 - offsetY))
                        raw.add(GridPoint(width - 1 - offsetX + dx, height - 1 - offsetY))
                    }
                    for (dy in -1..1) {
                        raw.add(GridPoint(offsetX, offsetY + dy))
                        raw.add(GridPoint(width - 1 - offsetX, offsetY + dy))
                        raw.add(GridPoint(offsetX, height - 1 - offsetY + dy))
                        raw.add(GridPoint(width - 1 - offsetX, height - 1 - offsetY + dy))
                    }
                }
                LabyrinthLayout.TWIN_GATES -> {
                    val gateYTop = max(3, height / 4)
                    val gateYBot = height - 1 - gateYTop
                    for (x in 3 until (width - 3)) {
                        if (x !in (midX - 2)..(midX + 1)) {
                            raw.add(GridPoint(x, gateYTop))
                            raw.add(GridPoint(x, gateYBot))
                        }
                    }
                }
                LabyrinthLayout.CENTRAL_VAULT -> {
                    val leftX = max(3, width / 4)
                    val rightX = width - 1 - leftX
                    for (y in 3 until (height - 3)) {
                        if (y !in (midY - 2)..(midY + 2)) {
                            raw.add(GridPoint(leftX, y))
                            raw.add(GridPoint(rightX, y))
                        }
                    }
                    for (x in (leftX + 2)..(rightX - 2)) {
                        raw.add(GridPoint(x, 3))
                        raw.add(GridPoint(x, height - 4))
                    }
                }
                LabyrinthLayout.CORNER_BUNKERS -> {
                    val inset = 2
                    val arm = max(3, minOf(width, height) / 5)
                    for (i in 0 until arm) {
                        raw.add(GridPoint(inset + i, inset))
                        raw.add(GridPoint(inset, inset + i))
                        raw.add(GridPoint(width - 1 - inset - i, inset))
                        raw.add(GridPoint(width - 1 - inset, inset + i))
                        raw.add(GridPoint(inset + i, height - 1 - inset))
                        raw.add(GridPoint(inset, height - 1 - inset - i))
                        raw.add(GridPoint(width - 1 - inset - i, height - 1 - inset))
                        raw.add(GridPoint(width - 1 - inset, height - 1 - inset - i))
                    }
                }
            }

            return raw.filterTo(mutableSetOf()) { pt ->
                pt.isInside(width, height) && pt !in safeZone
            }
        }

        fun generatePortals(
            width: Int,
            height: Int,
            obstacles: Set<GridPoint>,
            safeZone: Set<GridPoint>
        ): List<PortalPair> {
            val marginX = max(2, width / 5)
            val marginY = max(2, height / 5)

            val candidateA1 = GridPoint(marginX, marginY)
            val candidateB1 = GridPoint(width - 1 - marginX, height - 1 - marginY)

            val candidateA2 = GridPoint(width - 1 - marginX, marginY)
            val candidateB2 = GridPoint(marginX, height - 1 - marginY)

            val pairs = mutableListOf<PortalPair>()
            if (candidateA1 !in obstacles && candidateB1 !in obstacles &&
                candidateA1 !in safeZone && candidateB1 !in safeZone
            ) {
                pairs.add(
                    PortalPair(
                        id = 1,
                        entryA = candidateA1,
                        entryB = candidateB1,
                        colorHex = 0xFF00E5FFL
                    )
                )
            }
            if (candidateA2 !in obstacles && candidateB2 !in obstacles &&
                candidateA2 !in safeZone && candidateB2 !in safeZone
            ) {
                pairs.add(
                    PortalPair(
                        id = 2,
                        entryA = candidateA2,
                        entryB = candidateB2,
                        colorHex = 0xFFFF2A85L
                    )
                )
            }
            return pairs
        }
    }
}
