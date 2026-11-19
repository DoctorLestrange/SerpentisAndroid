package com.example

import com.example.engine.SnakeGameEngine
import com.example.model.ActiveBuff
import com.example.model.DeathReason
import com.example.model.Difficulty
import com.example.model.Direction
import com.example.model.FoodType
import com.example.model.GameMode
import com.example.model.GameSettings
import com.example.model.GameStatus
import com.example.model.GridPoint
import com.example.model.PortalPair
import com.example.model.SpawnedFood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ExampleUnitTest {

    private val engine = SnakeGameEngine(Random(42))

    @Test
    fun `turn queue prevents 180 degree double-input self collision and buffers valid cornering`() {
        val settings = GameSettings(gameMode = GameMode.CLASSIC)
        var state = engine.createNewGame(settings).copy(status = GameStatus.RUNNING)
        assertEquals(Direction.RIGHT, state.direction)

        // Rapidly input UP then LEFT before a single tick happens
        val (s1, accepted1) = engine.enqueueDirection(state, Direction.UP)
        assertTrue(accepted1)
        val (s2, accepted2) = engine.enqueueDirection(s1, Direction.LEFT)
        assertTrue(accepted2)

        // Direct opposite of last queued (RIGHT is opposite of LEFT) must be rejected
        val (_, acceptedOpposite) = engine.enqueueDirection(s2, Direction.RIGHT)
        assertFalse(acceptedOpposite)

        // A 3rd perpendicular turn (DOWN) before any tick happens must also be rejected so L-swipes never bite the snake
        val (_, acceptedThirdMove) = engine.enqueueDirection(s2, Direction.DOWN)
        assertFalse(acceptedThirdMove)
        assertEquals(2, s2.queuedDirections.size)

        // Tick 1 pops UP
        state = engine.stepTick(s2, settings).state
        assertEquals(Direction.UP, state.direction)
        assertEquals(GameStatus.RUNNING, state.status)

        // Tick 2 pops LEFT (completing a safe 2-tick U-turn without self collision)
        state = engine.stepTick(state, settings).state
        assertEquals(Direction.LEFT, state.direction)
        assertEquals(GameStatus.RUNNING, state.status)
    }

    @Test
    fun `stepping into vacating tail tip does not trigger false self-collision`() {
        val settings = GameSettings(gameMode = GameMode.CLASSIC)
        val loopSnake = listOf(
            GridPoint(5, 5), // head
            GridPoint(6, 5),
            GridPoint(6, 4),
            GridPoint(5, 4)  // tail tip (will vacate on non-growing move)
        )
        val state = engine.createNewGame(settings).copy(
            snake = loopSnake,
            direction = Direction.UP,
            foods = listOf(SpawnedFood(99, GridPoint(1, 1), FoodType.CYBER_APPLE, 0, null)),
            pendingGrowth = 0,
            status = GameStatus.RUNNING
        )

        val next = engine.stepTick(state, settings).state
        assertEquals(GameStatus.RUNNING, next.status)
        assertEquals(GridPoint(5, 4), next.snake.first())
        assertEquals(4, next.snake.size)
    }

    @Test
    fun `eating cyber apple increases score, length, and persists isNewHighScore across subsequent ticks`() {
        val settings = GameSettings(gameMode = GameMode.CLASSIC)
        val base = engine.createNewGame(settings, highScoreForMode = 5)
        val head = base.snake.first()
        val applePos = head.step(Direction.RIGHT)
        val stateWithFood = base.copy(
            status = GameStatus.RUNNING,
            foods = listOf(SpawnedFood(1L, applePos, FoodType.CYBER_APPLE, 0, null))
        )

        // Tick 1: Eat apple -> score exceeds initialHighScoreForMode (5)
        val afterEat = engine.stepTick(stateWithFood, settings).state
        assertEquals(1, afterEat.applesEaten)
        assertEquals(4, afterEat.snake.size)
        assertTrue(afterEat.score >= 10)
        assertTrue(afterEat.isNewHighScore)
        assertNotEquals(applePos, afterEat.foods.first().position)

        // Tick 2: Normal step without eating -> isNewHighScore must remain true!
        val afterNormalStep = engine.stepTick(afterEat, settings).state
        assertTrue(afterNormalStep.isNewHighScore)
    }

    @Test
    fun `zen mode wraps across outer perimeter walls seamlessly`() {
        val settings = GameSettings(gameMode = GameMode.ZEN)
        val base = engine.createNewGame(settings)
        val rightEdgeSnake = listOf(
            GridPoint(base.boardWidth - 1, 5),
            GridPoint(base.boardWidth - 2, 5),
            GridPoint(base.boardWidth - 3, 5)
        )
        val state = base.copy(
            status = GameStatus.RUNNING,
            snake = rightEdgeSnake,
            direction = Direction.RIGHT
        )

        val next = engine.stepTick(state, settings).state
        assertEquals(GameStatus.RUNNING, next.status)
        assertEquals(GridPoint(0, 5), next.snake.first())
    }

    @Test
    fun `portal flux mode teleports head, awards bonus flux points, and avoids infinite loop`() {
        val settings = GameSettings(gameMode = GameMode.PORTAL_FLUX)
        val base = engine.createNewGame(settings)
        val portal = PortalPair(1, GridPoint(10, 5), GridPoint(3, 14), 0xFF00E5FFL)
        val snake = listOf(GridPoint(9, 5), GridPoint(8, 5), GridPoint(7, 5))
        val state = base.copy(
            status = GameStatus.RUNNING,
            snake = snake,
            direction = Direction.RIGHT,
            portals = listOf(portal),
            foods = listOf(SpawnedFood(1L, GridPoint(1, 1), FoodType.CYBER_APPLE, 0, null))
        )

        // Step 1: Head enters (10,5), warps to (3,14), and earns Flux bonus points
        val step1 = engine.stepTick(state, settings).state
        assertEquals(GridPoint(3, 14), step1.snake.first())
        assertEquals(1, step1.portalsUsed)
        assertTrue(step1.score >= 15)

        // Step 2: Head steps off (3,14) to (4,14) cleanly
        val step2 = engine.stepTick(step1, settings).state
        assertEquals(GridPoint(4, 14), step2.snake.first())
        assertEquals(1, step2.portalsUsed)
    }

    @Test
    fun `phase ghost buff allows passing safely through obstacles`() {
        val settings = GameSettings(gameMode = GameMode.LABYRINTH)
        val base = engine.createNewGame(settings)
        val obstaclePoint = GridPoint(10, 10)
        val snake = listOf(GridPoint(9, 10), GridPoint(8, 10), GridPoint(7, 10))
        val stateWithGhost = base.copy(
            status = GameStatus.RUNNING,
            snake = snake,
            direction = Direction.RIGHT,
            obstacles = setOf(obstaclePoint),
            activeBuffs = listOf(ActiveBuff(FoodType.PHASE_GHOST, 10, 50))
        )

        val next = engine.stepTick(stateWithGhost, settings).state
        assertEquals(GameStatus.RUNNING, next.status)
        assertEquals(obstaclePoint, next.snake.first())
        assertEquals(DeathReason.NONE, next.deathReason)
    }

    @Test
    fun `computeFullAreaDimensions scales grid to fill rectangular play area with square cells`() {
        val baseSize = com.example.model.BoardSize.STANDARD // 20x20
        // Portrait play area (1000px wide by 1500px tall -> 1:1.5 aspect ratio)
        val (portraitW, portraitH) = SnakeGameEngine.computeFullAreaDimensions(baseSize, 1000, 1500)
        assertEquals(20, portraitW)
        assertEquals(30, portraitH)

        // Landscape play area (1600px wide by 800px tall -> 2:1 aspect ratio)
        val (landscapeW, landscapeH) = SnakeGameEngine.computeFullAreaDimensions(baseSize, 1600, 800)
        assertEquals(40, landscapeW)
        assertEquals(20, landscapeH)
    }

    @Test
    fun `resizeBoardPreservingState preserves active run score and clamps coordinates safely`() {
        val settings = GameSettings(gameMode = GameMode.CLASSIC, fullAreaGrid = true)
        val initial = engine.createNewGame(settings, overrideWidth = 18, overrideHeight = 26).copy(
            status = GameStatus.RUNNING,
            score = 120,
            applesEaten = 6
        )
        val resized = engine.resizeBoardPreservingState(initial, settings, newWidth = 18, newHeight = 18)
        assertEquals(18, resized.boardWidth)
        assertEquals(18, resized.boardHeight)
        assertEquals(120, resized.score)
        assertEquals(6, resized.applesEaten)
        assertEquals(GameStatus.RUNNING, resized.status)
        assertTrue(resized.snake.all { it.x in 0 until 18 && it.y in 0 until 18 })
    }

    @Test
    fun `difficulty poison defaults enable poison on Hyper and Overclocked only`() {
        assertFalse(Difficulty.CHILL.defaultPoisonEnabled)
        assertFalse(Difficulty.ARCADE.defaultPoisonEnabled)
        assertTrue(Difficulty.HYPER.defaultPoisonEnabled)
        assertTrue(Difficulty.OVERCLOCKED.defaultPoisonEnabled)
    }

    @Test
    fun `magnet buff pulls non-poison food 1 tile closer per tick within radius and never pulls poison`() {
        val settings = GameSettings(gameMode = GameMode.CLASSIC, poisonEnabled = true)
        val base = engine.createNewGame(settings)
        // Snake moves RIGHT from (5,10) -> candidateHead will be (6,10)
        val snake = listOf(GridPoint(5, 10), GridPoint(4, 10), GridPoint(3, 10))
        // Apple at (9,10) is 3 tiles away from candidateHead (6,10), inside radius 4 -> should pull to (8,10)
        val apple = SpawnedFood(10L, GridPoint(9, 10), FoodType.CYBER_APPLE, 0, null)
        // Poison skull at (6,13) is 3 tiles away from candidateHead (6,10), inside radius 4 -> MUST NOT move!
        val poison = SpawnedFood(11L, GridPoint(6, 13), FoodType.POISON_SKULL, 0, 30)

        val state = base.copy(
            status = GameStatus.RUNNING,
            snake = snake,
            direction = Direction.RIGHT,
            foods = listOf(apple, poison),
            activeBuffs = listOf(ActiveBuff(FoodType.MAGNET_CORE, 20, 55))
        )

        val next = engine.stepTick(state, settings).state
        val movedApple = next.foods.firstOrNull { it.id == 10L }
        val unmovedPoison = next.foods.firstOrNull { it.id == 11L }

        assertNotNull(movedApple)
        assertEquals(GridPoint(8, 10), movedApple?.position)

        assertNotNull(unmovedPoison)
        assertEquals(GridPoint(6, 13), unmovedPoison?.position)
    }

    @Test
    fun `poison skull spawns only when enabled, max 1 on grid, safe from head, relocates on expiry, and kills on contact`() {
        // 1. When poisonEnabled is false, no poison spawns
        val disabledSettings = GameSettings(gameMode = GameMode.CLASSIC, poisonEnabled = false)
        val disabledState = engine.createNewGame(disabledSettings)
        assertNull(disabledState.foods.firstOrNull { it.type == FoodType.POISON_SKULL })

        // 2. When poisonEnabled is true, exactly 1 poison spawns at a safe distance from head
        val enabledSettings = GameSettings(gameMode = GameMode.CLASSIC, poisonEnabled = true)
        val enabledState = engine.createNewGame(enabledSettings).copy(status = GameStatus.RUNNING)
        val poisons = enabledState.foods.filter { it.type == FoodType.POISON_SKULL }
        assertEquals(1, poisons.size)

        val firstPoisonPos = poisons.first().position
        val head = enabledState.snake.first()
        assertTrue(firstPoisonPos.manhattanTo(head) >= 5)
        assertTrue(firstPoisonPos.chebyshevTo(head) >= 4)

        // 3. When poison expires (expiresAtTick <= nextTick), it despawns and after cooldown relocates to a different tile
        val expiringState = enabledState.copy(
            foods = enabledState.foods.map {
                if (it.type == FoodType.POISON_SKULL) it.copy(expiresAtTick = enabledState.currentTick + 1) else it
            }
        )
        val afterExpire = engine.stepTick(expiringState, enabledSettings).state
        assertEquals(0, afterExpire.foods.count { it.type == FoodType.POISON_SKULL })
        assertEquals(firstPoisonPos, afterExpire.lastPoisonPosition)

        // Advance ticks until nextPoisonSpawnTick triggers a fresh poison skull
        var current = afterExpire
        repeat(SnakeGameEngine.POISON_RESPAWN_COOLDOWN_TICKS + 1) {
            current = engine.stepTick(current, enabledSettings).state
        }
        val relocatedPoisons = current.foods.filter { it.type == FoodType.POISON_SKULL }
        assertEquals(1, relocatedPoisons.size)
        assertNotEquals(firstPoisonPos, relocatedPoisons.first().position)

        // 4. Direct contact with poison skull triggers instant GAME_OVER with DeathReason.POISON_CONSUMED
        val currentHead = current.snake.first()
        val dir = current.direction
        val lethalPos = currentHead.step(dir)
        val lethalState = current.copy(
            foods = current.foods.filterNot { it.type == FoodType.POISON_SKULL } +
                SpawnedFood(999L, lethalPos, FoodType.POISON_SKULL, current.currentTick, current.currentTick + 40)
        )
        val fatalOutcome = engine.stepTick(lethalState, enabledSettings)
        assertTrue(fatalOutcome.events.any { it is com.example.engine.GameEvent.GameEnded })
        assertEquals(GameStatus.GAME_OVER, fatalOutcome.state.status)
        assertEquals(DeathReason.POISON_CONSUMED, fatalOutcome.state.deathReason)
    }

    @Test
    fun `collecting magnet core activates magnet buff and phase ghost protects against poison skull`() {
        val settings = GameSettings(gameMode = GameMode.CLASSIC, poisonEnabled = true)
        val base = engine.createNewGame(settings)
        val snake = listOf(GridPoint(5, 10), GridPoint(4, 10), GridPoint(3, 10))
        // Place MAGNET_CORE directly ahead at (6,10) and CYBER_APPLE at (9,10)
        val magnetOrb = SpawnedFood(50L, GridPoint(6, 10), FoodType.MAGNET_CORE, 0, 50)
        val apple = SpawnedFood(51L, GridPoint(9, 10), FoodType.CYBER_APPLE, 0, null)

        val stateBeforePickup = base.copy(
            status = GameStatus.RUNNING,
            snake = snake,
            direction = Direction.RIGHT,
            foods = listOf(magnetOrb, apple)
        )

        val afterPickup = engine.stepTick(stateBeforePickup, settings).state
        assertTrue(afterPickup.isMagnetActive)
        assertEquals(1, afterPickup.powerUpsCollected)
        // Apple at (9,10) was pulled 1 tile closer to (8,10) on the pickup tick itself
        assertEquals(GridPoint(8, 10), afterPickup.foods.firstOrNull { it.id == 51L }?.position)

        // Verify PHASE_GHOST allows passing safely over a POISON_SKULL without dying
        val ghostHead = afterPickup.snake.first()
        val poisonAhead = ghostHead.step(afterPickup.direction)
        val ghostState = afterPickup.copy(
            activeBuffs = afterPickup.activeBuffs + ActiveBuff(FoodType.PHASE_GHOST, 10, 48),
            foods = afterPickup.foods.filterNot { it.type == FoodType.POISON_SKULL } +
                SpawnedFood(777L, poisonAhead, FoodType.POISON_SKULL, afterPickup.currentTick, afterPickup.currentTick + 30)
        )
        val afterGhostStep = engine.stepTick(ghostState, settings).state
        assertEquals(GameStatus.RUNNING, afterGhostStep.status)
        assertEquals(DeathReason.NONE, afterGhostStep.deathReason)
    }
}
