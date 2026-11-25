package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AchievementCatalog
import com.example.model.ActiveBuff
import com.example.model.AppAccent
import com.example.model.AppThemeMode
import com.example.model.ArenaTheme
import com.example.model.BoardSize
import com.example.model.ControlScheme
import com.example.model.DeathReason
import com.example.model.Difficulty
import com.example.model.Direction
import com.example.model.FoodType
import com.example.model.GameMode
import com.example.model.GameSettings
import com.example.model.GameState
import com.example.model.GameStatus
import com.example.model.GridPoint
import com.example.model.LabyrinthLayout
import com.example.model.PortalPair
import com.example.model.SnakeSkin
import com.example.model.SpawnedFood
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

data class CareerStats(
    val totalRuns: Int = 0,
    val bestScoreOverall: Int = 0,
    val totalApplesEaten: Int = 0,
    val totalGoldenEaten: Int = 0,
    val longestSnakeEver: Int = 3,
    val highestComboEver: Int = 1,
    val totalPlayTimeMillis: Long = 0L,
    val highScoreByMode: Map<GameMode, Int> = emptyMap()
)

class GameRepository(
    private val gameDao: GameDao,
    context: Context
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("serpentis_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettingsFromPrefs())
    val settingsFlow: StateFlow<GameSettings> = _settingsFlow.asStateFlow()

    val allRunsByScore: Flow<List<RunRecordEntity>> = gameDao.getAllRunsSortedByScore()
    val allRunsRecent: Flow<List<RunRecordEntity>> = gameDao.getAllRunsRecent()
    val savedGameFlow: Flow<SavedGameEntity?> = gameDao.getSavedGameFlow()
    val achievementsFlow: Flow<List<AchievementEntity>> = gameDao.getAllAchievementsFlow()

    fun updateSettings(transform: (GameSettings) -> GameSettings): GameSettings {
        val updated = transform(_settingsFlow.value)
        _settingsFlow.value = updated
        saveSettingsToPrefs(updated)
        return updated
    }

    fun resetSettingsToDefaults(): GameSettings {
        val defaults = GameSettings()
        _settingsFlow.value = defaults
        saveSettingsToPrefs(defaults)
        return defaults
    }

    suspend fun recordCompletedRun(state: GameState, settings: GameSettings) {
        val resolvedBoardSize = BoardSize.fromDimensions(state.boardWidth, state.boardHeight)
        val record = RunRecordEntity(
            score = state.score.coerceAtLeast(0),
            gameMode = state.mode.name,
            difficulty = state.difficulty.name,
            boardSize = resolvedBoardSize.name,
            snakeSkin = settings.snakeSkin.name,
            applesEaten = state.applesEaten.coerceAtLeast(0),
            goldenEaten = state.goldenEaten.coerceAtLeast(0),
            powerUpsCollected = state.powerUpsCollected.coerceAtLeast(0),
            portalsUsed = state.portalsUsed.coerceAtLeast(0),
            maxCombo = state.maxComboReached.coerceIn(1, 8),
            finalLength = state.snake.size.coerceAtLeast(1),
            durationMillis = state.elapsedPlayMillis.coerceAtLeast(0L),
            deathReason = state.deathReason.name,
            isVictory = state.status == GameStatus.VICTORY
        )
        gameDao.insertRunRecord(record)
        gameDao.clearSavedGame()
        evaluateAchievementsOnRunEnd(state)
    }

    suspend fun saveActiveGame(state: GameState) {
        if (state.status != GameStatus.RUNNING && state.status != GameStatus.PAUSED) return
        if (state.snake.size < 2) return

        val entity = SavedGameEntity(
            slotId = 1,
            modeName = state.mode.name,
            difficultyName = state.difficulty.name,
            boardWidth = state.boardWidth,
            boardHeight = state.boardHeight,
            labyrinthLayoutName = state.labyrinthLayout.name,
            snakeEncoded = encodePoints(state.snake),
            directionName = state.direction.name,
            obstaclesEncoded = encodePoints(state.obstacles),
            portalsEncoded = encodePortals(state.portals),
            foodsEncoded = encodeFoods(state.foods),
            buffsEncoded = encodeBuffs(state.activeBuffs),
            score = state.score,
            applesEaten = state.applesEaten,
            goldenEaten = state.goldenEaten,
            powerUpsCollected = state.powerUpsCollected,
            portalsUsed = state.portalsUsed,
            comboCount = state.comboCount,
            maxComboReached = state.maxComboReached,
            ticksSinceLastEat = state.ticksSinceLastEat,
            currentTick = state.currentTick,
            currentTickIntervalMs = state.currentTickIntervalMs,
            remainingBlitzMillis = state.remainingBlitzMillis,
            elapsedPlayMillis = state.elapsedPlayMillis,
            pendingGrowth = state.pendingGrowth
        )
        gameDao.upsertSavedGame(entity)
    }

    suspend fun clearSavedGame() {
        gameDao.clearSavedGame()
    }

    suspend fun clearRunHistory() {
        gameDao.clearAllRuns()
    }

    fun restoreSavedGame(saved: SavedGameEntity, highScoreForMode: Int): GameState? {
        val width = saved.boardWidth.coerceIn(10, 48)
        val height = saved.boardHeight.coerceIn(10, 48)

        val snake = decodePoints(saved.snakeEncoded).filter { it.isInside(width, height) }
        if (snake.size < 2) return null

        val mode = runCatching { GameMode.valueOf(saved.modeName) }.getOrDefault(GameMode.CLASSIC)
        val difficulty = runCatching { Difficulty.valueOf(saved.difficultyName) }.getOrDefault(Difficulty.ARCADE)
        val layout = runCatching { LabyrinthLayout.valueOf(saved.labyrinthLayoutName) }
            .getOrDefault(LabyrinthLayout.CROSSHAIR_PILLARS)
        val direction = runCatching { Direction.valueOf(saved.directionName) }.getOrDefault(Direction.RIGHT)

        val obstacles = decodePoints(saved.obstaclesEncoded)
            .filterTo(mutableSetOf()) { it.isInside(width, height) }
        val portals = decodePortals(saved.portalsEncoded)
            .filter { it.entryA.isInside(width, height) && it.entryB.isInside(width, height) }
        val foods = decodeFoods(saved.foodsEncoded)
            .filter { it.position.isInside(width, height) }
        val buffs = decodeBuffs(saved.buffsEncoded)
            .filter { it.remainingTicks > 0 && it.totalTicks > 0 }

        val safeScore = saved.score.coerceAtLeast(0)
        val safeHigh = max(highScoreForMode.coerceAtLeast(0), safeScore)

        return GameState(
            mode = mode,
            difficulty = difficulty,
            boardWidth = width,
            boardHeight = height,
            labyrinthLayout = layout,
            snake = snake,
            previousSnake = snake,
            direction = direction,
            queuedDirections = emptyList(),
            obstacles = obstacles,
            portals = portals,
            foods = foods,
            activeBuffs = buffs,
            recentBursts = emptyList(),
            score = safeScore,
            initialHighScoreForMode = highScoreForMode.coerceAtLeast(0),
            highScoreForMode = safeHigh,
            applesEaten = saved.applesEaten.coerceAtLeast(0),
            goldenEaten = saved.goldenEaten.coerceAtLeast(0),
            powerUpsCollected = saved.powerUpsCollected.coerceAtLeast(0),
            portalsUsed = saved.portalsUsed.coerceAtLeast(0),
            comboCount = saved.comboCount.coerceIn(1, 8),
            maxComboReached = saved.maxComboReached.coerceIn(1, 8),
            ticksSinceLastEat = saved.ticksSinceLastEat.coerceAtLeast(0),
            comboWindowTicks = max(width, height) + 2,
            currentTick = saved.currentTick.coerceAtLeast(0),
            currentTickIntervalMs = saved.currentTickIntervalMs.coerceIn(40L, 400L),
            remainingBlitzMillis = saved.remainingBlitzMillis.coerceIn(1_000L, 120_000L),
            elapsedPlayMillis = saved.elapsedPlayMillis.coerceAtLeast(0L),
            pendingGrowth = saved.pendingGrowth.coerceIn(0, 20),
            status = GameStatus.PAUSED,
            deathReason = DeathReason.NONE,
            isNewHighScore = safeScore > highScoreForMode && safeScore > 0
        )
    }

    private suspend fun evaluateAchievementsOnRunEnd(state: GameState) {
        val existingMap = gameDao.getAllAchievementsSnapshot().associateBy { it.id }
        val now = System.currentTimeMillis()
        val updated = AchievementCatalog.ALL.map { def ->
            val prev = existingMap[def.id]
            val prevVal = prev?.currentValue ?: 0
            val newVal = when (def.id) {
                "first_byte" -> prevVal + state.applesEaten
                "combo_striker" -> max(prevVal, state.maxComboReached)
                "golden_hunter" -> prevVal + state.goldenEaten
                "serpent_centurion" -> max(prevVal, state.snake.size)
                "labyrinth_master" -> if (state.mode == GameMode.LABYRINTH) max(prevVal, state.score) else prevVal
                "portal_jumper" -> prevVal + state.portalsUsed
                "hyper_survivor" -> if (state.difficulty == Difficulty.HYPER || state.difficulty == Difficulty.OVERCLOCKED) {
                    max(prevVal, state.score)
                } else prevVal
                "blitz_sprinter" -> if (state.mode == GameMode.TIME_BLITZ) max(prevVal, state.score) else prevVal
                "arcade_veteran" -> prevVal + 1
                "high_roller" -> max(prevVal, state.score)
                else -> prevVal
            }
            val unlocked = (prev?.isUnlocked == true) || (newVal >= def.targetValue)
            AchievementEntity(
                id = def.id,
                currentValue = newVal,
                isUnlocked = unlocked,
                unlockedAtTimestamp = if (unlocked) (prev?.unlockedAtTimestamp ?: now) else null
            )
        }
        gameDao.upsertAchievements(updated)
    }

    private fun loadSettingsFromPrefs(): GameSettings {
        val mode = prefs.getString("gameMode", null)?.let {
            runCatching { GameMode.valueOf(it) }.getOrNull()
        } ?: GameMode.CLASSIC
        val difficulty = prefs.getString("difficulty", null)?.let {
            runCatching { Difficulty.valueOf(it) }.getOrNull()
        } ?: Difficulty.ARCADE
        val boardSize = prefs.getString("boardSize", null)?.let {
            runCatching { BoardSize.valueOf(it) }.getOrNull()
        } ?: BoardSize.STANDARD
        val controlScheme = prefs.getString("controlScheme", null)?.let {
            runCatching { ControlScheme.valueOf(it) }.getOrNull()
        } ?: ControlScheme.DPAD
        val appThemeMode = prefs.getString("appThemeMode", null)?.let {
            runCatching { AppThemeMode.valueOf(it) }.getOrNull()
        } ?: AppThemeMode.DARK_GRAPHITE
        val appAccent = prefs.getString("appAccent", null)?.let {
            runCatching { AppAccent.valueOf(it) }.getOrNull()
        } ?: AppAccent.EMERALD_CYAN
        val snakeSkin = prefs.getString("snakeSkin", null)?.let {
            runCatching { SnakeSkin.valueOf(it) }.getOrNull()
        } ?: SnakeSkin.CYBER_EMERALD
        val arenaTheme = prefs.getString("arenaTheme", null)?.let {
            runCatching { ArenaTheme.valueOf(it) }.getOrNull()
        } ?: ArenaTheme.GRAPHITE_MINIMAL
        val labyrinthLayout = prefs.getString("labyrinthLayout", null)?.let {
            runCatching { LabyrinthLayout.valueOf(it) }.getOrNull()
        } ?: LabyrinthLayout.CROSSHAIR_PILLARS

        return GameSettings(
            gameMode = mode,
            difficulty = difficulty,
            boardSize = boardSize,
            fullAreaGrid = prefs.getBoolean("fullAreaGrid", false),
            poisonEnabled = prefs.getBoolean("poisonEnabled", difficulty.defaultPoisonEnabled),
            controlScheme = controlScheme,
            appThemeMode = appThemeMode,
            appAccent = appAccent,
            useDynamicColors = prefs.getBoolean("useDynamicColors", false),
            snakeSkin = snakeSkin,
            arenaTheme = arenaTheme,
            labyrinthLayout = labyrinthLayout,
            dynamicSpeedScaling = prefs.getBoolean("dynamicSpeedScaling", true),
            soundEnabled = prefs.getBoolean("soundEnabled", true),
            hapticsEnabled = prefs.getBoolean("hapticsEnabled", true),
            showGridLines = prefs.getBoolean("showGridLines", true),
            smoothMovement = prefs.getBoolean("smoothMovement", true),
            swipeSensitivity = prefs.getFloat("swipeSensitivity", 1.0f).coerceIn(0.5f, 1.5f)
        )
    }

    private fun saveSettingsToPrefs(s: GameSettings) {
        prefs.edit()
            .putString("gameMode", s.gameMode.name)
            .putString("difficulty", s.difficulty.name)
            .putString("boardSize", s.boardSize.name)
            .putBoolean("fullAreaGrid", s.fullAreaGrid)
            .putBoolean("poisonEnabled", s.poisonEnabled)
            .putString("controlScheme", s.controlScheme.name)
            .putString("appThemeMode", s.appThemeMode.name)
            .putString("appAccent", s.appAccent.name)
            .putBoolean("useDynamicColors", s.useDynamicColors)
            .putString("snakeSkin", s.snakeSkin.name)
            .putString("arenaTheme", s.arenaTheme.name)
            .putString("labyrinthLayout", s.labyrinthLayout.name)
            .putBoolean("dynamicSpeedScaling", s.dynamicSpeedScaling)
            .putBoolean("soundEnabled", s.soundEnabled)
            .putBoolean("hapticsEnabled", s.hapticsEnabled)
            .putBoolean("showGridLines", s.showGridLines)
            .putBoolean("smoothMovement", s.smoothMovement)
            .putFloat("swipeSensitivity", s.swipeSensitivity)
            .apply()
    }

    companion object {
        fun computeCareerStats(runs: List<RunRecordEntity>): CareerStats {
            if (runs.isEmpty()) return CareerStats()
            val byMode = GameMode.entries.associateWith { mode ->
                runs.filter { it.gameMode == mode.name }.maxOfOrNull { it.score } ?: 0
            }
            return CareerStats(
                totalRuns = runs.size,
                bestScoreOverall = runs.maxOfOrNull { it.score } ?: 0,
                totalApplesEaten = runs.sumOf { it.applesEaten },
                totalGoldenEaten = runs.sumOf { it.goldenEaten },
                longestSnakeEver = runs.maxOfOrNull { it.finalLength } ?: 3,
                highestComboEver = runs.maxOfOrNull { it.maxCombo } ?: 1,
                totalPlayTimeMillis = runs.sumOf { it.durationMillis },
                highScoreByMode = byMode
            )
        }

        private fun encodePoints(points: Collection<GridPoint>): String =
            points.joinToString(";") { "${it.x},${it.y}" }

        private fun decodePoints(encoded: String): List<GridPoint> {
            if (encoded.isBlank()) return emptyList()
            return encoded.split(";").mapNotNull { token ->
                val parts = token.split(",")
                if (parts.size == 2) {
                    val x = parts[0].toIntOrNull()
                    val y = parts[1].toIntOrNull()
                    if (x != null && y != null) GridPoint(x, y) else null
                } else null
            }
        }

        private fun encodePortals(portals: List<PortalPair>): String =
            portals.joinToString(";") { p ->
                "${p.id},${p.entryA.x},${p.entryA.y},${p.entryB.x},${p.entryB.y},${p.colorHex}"
            }

        private fun decodePortals(encoded: String): List<PortalPair> {
            if (encoded.isBlank()) return emptyList()
            return encoded.split(";").mapNotNull { token ->
                val parts = token.split(",")
                if (parts.size == 6) {
                    val id = parts[0].toIntOrNull() ?: return@mapNotNull null
                    val ax = parts[1].toIntOrNull() ?: return@mapNotNull null
                    val ay = parts[2].toIntOrNull() ?: return@mapNotNull null
                    val bx = parts[3].toIntOrNull() ?: return@mapNotNull null
                    val by = parts[4].toIntOrNull() ?: return@mapNotNull null
                    val color = parts[5].toLongOrNull() ?: return@mapNotNull null
                    PortalPair(id, GridPoint(ax, ay), GridPoint(bx, by), color)
                } else null
            }
        }

        private fun encodeFoods(foods: List<SpawnedFood>): String =
            foods.joinToString(";") { f ->
                "${f.id},${f.position.x},${f.position.y},${f.type.name},${f.spawnedAtTick},${f.expiresAtTick ?: -1}"
            }

        private fun decodeFoods(encoded: String): List<SpawnedFood> {
            if (encoded.isBlank()) return emptyList()
            return encoded.split(";").mapNotNull { token ->
                val parts = token.split(",")
                if (parts.size == 6) {
                    val id = parts[0].toLongOrNull() ?: return@mapNotNull null
                    val x = parts[1].toIntOrNull() ?: return@mapNotNull null
                    val y = parts[2].toIntOrNull() ?: return@mapNotNull null
                    val type = runCatching { FoodType.valueOf(parts[3]) }.getOrNull() ?: return@mapNotNull null
                    val spawnTick = parts[4].toIntOrNull() ?: 0
                    val expRaw = parts[5].toIntOrNull() ?: -1
                    SpawnedFood(id, GridPoint(x, y), type, spawnTick, if (expRaw >= 0) expRaw else null)
                } else null
            }
        }

        private fun encodeBuffs(buffs: List<ActiveBuff>): String =
            buffs.joinToString(";") { b ->
                "${b.type.name},${b.remainingTicks},${b.totalTicks}"
            }

        private fun decodeBuffs(encoded: String): List<ActiveBuff> {
            if (encoded.isBlank()) return emptyList()
            return encoded.split(";").mapNotNull { token ->
                val parts = token.split(",")
                if (parts.size == 3) {
                    val type = runCatching { FoodType.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
                    val rem = parts[1].toIntOrNull() ?: return@mapNotNull null
                    val tot = parts[2].toIntOrNull() ?: return@mapNotNull null
                    ActiveBuff(type, rem, tot)
                } else null
            }
        }
    }
}
