package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AchievementEntity
import com.example.data.CareerStats
import com.example.data.GameRepository
import com.example.data.RunRecordEntity
import com.example.data.SavedGameEntity
import com.example.engine.ArcadeFeedbackManager
import com.example.engine.GameEvent
import com.example.engine.SnakeGameEngine
import com.example.model.ArenaTheme
import com.example.model.BoardSize
import com.example.model.ControlScheme
import com.example.model.Difficulty
import com.example.model.Direction
import com.example.model.FoodType
import com.example.model.GameMode
import com.example.model.GameSettings
import com.example.model.GameState
import com.example.model.GameStatus
import com.example.model.LabyrinthLayout
import com.example.model.SnakeSkin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppDestination(val route: String) {
    HOME("home"),
    ARENA("arena"),
    WORKSHOP("workshop"),
    RECORDS("records"),
    SETTINGS("settings")
}

class GameViewModel(
    private val repository: GameRepository,
    private val feedbackManager: ArcadeFeedbackManager,
    private val engine: SnakeGameEngine = SnakeGameEngine()
) : ViewModel() {

    val settings: StateFlow<GameSettings> = repository.settingsFlow

    val runsByScore: StateFlow<List<RunRecordEntity>> = repository.allRunsByScore
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentRuns: StateFlow<List<RunRecordEntity>> = repository.allRunsRecent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val careerStats: StateFlow<CareerStats> = runsByScore
        .map { GameRepository.computeCareerStats(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CareerStats())

    val savedGame: StateFlow<SavedGameEntity?> = repository.savedGameFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val achievements: StateFlow<List<AchievementEntity>> = repository.achievementsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentDestination = MutableStateFlow(AppDestination.HOME)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    private val _selectedRecordsModeFilter = MutableStateFlow<GameMode?>(null)
    val selectedRecordsModeFilter: StateFlow<GameMode?> = _selectedRecordsModeFilter.asStateFlow()

    private var lastMeasuredAreaWidthPx: Int = 0
    private var lastMeasuredAreaHeightPx: Int = 0

    private val _gameState = MutableStateFlow(
        createInitialGameForCurrentSettings(repository.settingsFlow.value, 0)
    )
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _subTickFraction = MutableStateFlow(1f)
    val subTickFraction: StateFlow<Float> = _subTickFraction.asStateFlow()

    private var gameLoopJob: Job? = null

    init {
        viewModelScope.launch {
            careerStats.collectLatest { stats ->
                val current = _gameState.value
                val modeBest = stats.highScoreByMode[current.mode] ?: 0
                if (current.status == GameStatus.READY && current.highScoreForMode != modeBest) {
                    _gameState.value = current.copy(
                        initialHighScoreForMode = modeBest,
                        highScoreForMode = modeBest
                    )
                }
            }
        }
    }

    private fun resolveTargetGridDimensions(s: GameSettings): Pair<Int, Int> {
        return if (s.fullAreaGrid) {
            SnakeGameEngine.computeFullAreaDimensions(
                baseBoardSize = s.boardSize,
                areaWidthPx = lastMeasuredAreaWidthPx,
                areaHeightPx = lastMeasuredAreaHeightPx
            )
        } else {
            s.boardSize.columns to s.boardSize.rows
        }
    }

    private fun createInitialGameForCurrentSettings(
        s: GameSettings,
        highScore: Int
    ): GameState {
        val (w, h) = resolveTargetGridDimensions(s)
        return engine.createNewGame(
            settings = s,
            highScoreForMode = highScore,
            overrideWidth = w,
            overrideHeight = h
        )
    }

    fun onPlayAreaSizeChanged(widthPx: Int, heightPx: Int) {
        if (widthPx <= 0 || heightPx <= 0) return
        val prevW = lastMeasuredAreaWidthPx
        val prevH = lastMeasuredAreaHeightPx
        val changed = (widthPx != prevW || heightPx != prevH)
        lastMeasuredAreaWidthPx = widthPx
        lastMeasuredAreaHeightPx = heightPx

        if (changed && settings.value.fullAreaGrid) {
            val current = _gameState.value
            val isOrientationFlip = prevW > 0 && prevH > 0 && (widthPx > heightPx) != (prevW > prevH)
            val isInitialMeasure = prevW == 0 || prevH == 0
            // Never alter grid row/column count during an active/paused run unless initial measure or screen rotation
            if (current.status == GameStatus.READY || isInitialMeasure || isOrientationFlip) {
                val (targetCols, targetRows) = resolveTargetGridDimensions(settings.value)
                if (current.boardWidth != targetCols || current.boardHeight != targetRows) {
                    _gameState.value = engine.resizeBoardPreservingState(
                        state = current,
                        settings = settings.value,
                        newWidth = targetCols,
                        newHeight = targetRows
                    )
                }
            }
        }
    }

    fun navigateTo(destination: AppDestination) {
        if (_currentDestination.value == AppDestination.ARENA && destination != AppDestination.ARENA) {
            if (_gameState.value.status == GameStatus.RUNNING) {
                pauseGame()
            }
        }
        _currentDestination.value = destination
        if (destination == AppDestination.ARENA) {
            val status = _gameState.value.status
            if (status == GameStatus.GAME_OVER || status == GameStatus.VICTORY || status == GameStatus.TIME_UP) {
                prepareFreshArena()
            }
        }
    }

    fun prepareFreshArena(
        modeOverride: GameMode? = null,
        difficultyOverride: Difficulty? = null,
        clearExistingSave: Boolean = false
    ) {
        gameLoopJob?.cancel()
        if (clearExistingSave) {
            viewModelScope.launch {
                repository.clearSavedGame()
            }
        }
        val updatedSettings = repository.updateSettings { current ->
            val newDiff = difficultyOverride ?: current.difficulty
            val newPoison = if (difficultyOverride != null && difficultyOverride != current.difficulty) {
                difficultyOverride.defaultPoisonEnabled
            } else {
                current.poisonEnabled
            }
            current.copy(
                gameMode = modeOverride ?: current.gameMode,
                difficulty = newDiff,
                poisonEnabled = newPoison
            )
        }
        val modeHighScore = careerStats.value.highScoreByMode[updatedSettings.gameMode] ?: 0
        _gameState.value = createInitialGameForCurrentSettings(updatedSettings, modeHighScore)
        _subTickFraction.value = 1f
    }

    fun launchNewRun(
        modeOverride: GameMode? = null,
        difficultyOverride: Difficulty? = null,
        autoStart: Boolean = true
    ) {
        prepareFreshArena(modeOverride, difficultyOverride, clearExistingSave = true)
        _currentDestination.value = AppDestination.ARENA
        if (autoStart) {
            startOrResumeGame()
        }
    }

    fun startOrResumeGame() {
        val current = _gameState.value
        when (current.status) {
            GameStatus.READY, GameStatus.PAUSED -> {
                _gameState.value = current.copy(status = GameStatus.RUNNING)
                startGameLoop()
            }
            GameStatus.GAME_OVER, GameStatus.VICTORY, GameStatus.TIME_UP -> {
                prepareFreshArena(clearExistingSave = true)
                _gameState.value = _gameState.value.copy(status = GameStatus.RUNNING)
                startGameLoop()
            }
            GameStatus.RUNNING -> {
                if (gameLoopJob?.isActive != true) {
                    startGameLoop()
                }
            }
        }
    }

    fun pauseGame() {
        gameLoopJob?.cancel()
        val current = _gameState.value
        if (current.status == GameStatus.RUNNING) {
            val paused = current.copy(status = GameStatus.PAUSED)
            _gameState.value = paused
            _subTickFraction.value = 1f
            viewModelScope.launch {
                repository.saveActiveGame(paused)
            }
        }
    }

    fun togglePauseResume() {
        when (_gameState.value.status) {
            GameStatus.RUNNING -> pauseGame()
            GameStatus.PAUSED, GameStatus.READY -> startOrResumeGame()
            else -> Unit
        }
    }

    fun resumeFromSavedEntity(saved: SavedGameEntity? = savedGame.value) {
        val target = saved ?: return
        gameLoopJob?.cancel()
        val mode = runCatching { GameMode.valueOf(target.modeName) }.getOrDefault(GameMode.CLASSIC)
        val highScore = careerStats.value.highScoreByMode[mode] ?: 0
        val restored = repository.restoreSavedGame(target, highScore) ?: return

        val isNonSquare = restored.boardWidth != restored.boardHeight
        val restoredHasPoison = restored.foods.any { it.type == FoodType.POISON_SKULL }
        repository.updateSettings { current ->
            current.copy(
                gameMode = restored.mode,
                difficulty = restored.difficulty,
                boardSize = BoardSize.fromDimensions(restored.boardWidth, restored.boardHeight),
                fullAreaGrid = isNonSquare || current.fullAreaGrid,
                poisonEnabled = restoredHasPoison || current.poisonEnabled,
                labyrinthLayout = restored.labyrinthLayout
            )
        }

        _gameState.value = restored
        _subTickFraction.value = 1f
        _currentDestination.value = AppDestination.ARENA
    }

    fun discardSavedGame() {
        viewModelScope.launch {
            repository.clearSavedGame()
        }
    }

    fun onDirectionInput(direction: Direction) {
        val current = _gameState.value
        if (current.status == GameStatus.PAUSED) {
            val resumed = current.copy(status = GameStatus.RUNNING)
            val (updated, accepted) = engine.enqueueDirection(resumed, direction)
            _gameState.value = updated
            if (accepted) {
                feedbackManager.onTurn(settings.value.soundEnabled, settings.value.hapticsEnabled)
            }
            startGameLoop()
            return
        }

        val wasReady = current.status == GameStatus.READY
        val (updated, accepted) = engine.enqueueDirection(current, direction)
        _gameState.value = updated
        if (accepted) {
            feedbackManager.onTurn(settings.value.soundEnabled, settings.value.hapticsEnabled)
        }
        if (wasReady && updated.status == GameStatus.RUNNING) {
            startGameLoop()
        }
    }

    fun onRelativeTurnInput(clockwise: Boolean) {
        val current = _gameState.value
        val wasReady = current.status == GameStatus.READY || current.status == GameStatus.PAUSED
        val baseState = if (current.status == GameStatus.PAUSED) {
            current.copy(status = GameStatus.RUNNING)
        } else {
            current
        }
        val (updated, accepted) = engine.enqueueRelativeTurn(baseState, clockwise)
        _gameState.value = updated
        if (accepted) {
            feedbackManager.onTurn(settings.value.soundEnabled, settings.value.hapticsEnabled)
        }
        if (wasReady && updated.status == GameStatus.RUNNING) {
            startGameLoop()
        }
    }

    fun stepOneTick() {
        val current = _gameState.value
        if (current.status != GameStatus.RUNNING) return

        val currentSettings = settings.value
        val result = engine.stepTick(current, currentSettings)
        _gameState.value = result.state

        val sfx = currentSettings.soundEnabled
        val haptics = currentSettings.hapticsEnabled

        for (event in result.events) {
            when (event) {
                is GameEvent.AteFood -> {
                    if (event.foodType == FoodType.CYBER_APPLE) {
                        feedbackManager.onEatApple(event.newCombo, sfx, haptics)
                    } else {
                        feedbackManager.onEatGoldenOrPowerUp(sfx, haptics)
                    }
                }
                GameEvent.BonusFoodSpawned -> Unit
                is GameEvent.PortalWarped -> feedbackManager.onPortalWarp(sfx, haptics)
                GameEvent.Turned -> Unit
                GameEvent.BlitzWallPenalty -> feedbackManager.onPortalWarp(sfx, haptics)
                is GameEvent.GameEnded -> {
                    gameLoopJob?.cancel()
                    _subTickFraction.value = 1f
                    if (event.status == GameStatus.VICTORY) {
                        feedbackManager.onVictory(sfx, haptics)
                    } else {
                        feedbackManager.onGameOver(sfx, haptics)
                    }
                    viewModelScope.launch {
                        repository.recordCompletedRun(result.state, currentSettings)
                    }
                }
            }
        }
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (isActive && _gameState.value.status == GameStatus.RUNNING) {
                val intervalMs = _gameState.value.currentTickIntervalMs.coerceAtLeast(40L)
                if (settings.value.smoothMovement) {
                    val frameStepMs = 16L
                    val tickStartNs = System.nanoTime()
                    var virtualElapsedMs = 0L
                    while (isActive && _gameState.value.status == GameStatus.RUNNING) {
                        val wallElapsedMs = ((System.nanoTime() - tickStartNs) / 1_000_000L).coerceAtLeast(0L)
                        val effectiveElapsedMs = maxOf(virtualElapsedMs, wallElapsedMs)
                        if (effectiveElapsedMs >= intervalMs) break

                        _subTickFraction.value = (effectiveElapsedMs.toFloat() / intervalMs.toFloat()).coerceIn(0f, 1f)
                        val remainingMs = (intervalMs - effectiveElapsedMs).coerceAtLeast(1L)
                        val sleepTime = minOf(frameStepMs, remainingMs)
                        delay(sleepTime)
                        virtualElapsedMs += sleepTime
                    }
                } else {
                    _subTickFraction.value = 1f
                    delay(intervalMs)
                }

                if (isActive && _gameState.value.status == GameStatus.RUNNING) {
                    _subTickFraction.value = 0f
                    stepOneTick()
                }
            }
        }
    }

    fun setRecordsModeFilter(mode: GameMode?) {
        _selectedRecordsModeFilter.value = mode
    }

    fun setGameMode(mode: GameMode) {
        val updated = repository.updateSettings { it.copy(gameMode = mode) }
        if (_gameState.value.status == GameStatus.READY) {
            val highScore = careerStats.value.highScoreByMode[mode] ?: 0
            _gameState.value = createInitialGameForCurrentSettings(updated, highScore)
        }
    }

    fun setDifficulty(difficulty: Difficulty) {
        val updated = repository.updateSettings {
            it.copy(
                difficulty = difficulty,
                poisonEnabled = difficulty.defaultPoisonEnabled
            )
        }
        if (_gameState.value.status == GameStatus.READY) {
            val highScore = careerStats.value.highScoreByMode[updated.gameMode] ?: 0
            _gameState.value = createInitialGameForCurrentSettings(updated, highScore)
        }
    }

    fun setPoisonEnabled(enabled: Boolean) {
        val updated = repository.updateSettings { it.copy(poisonEnabled = enabled) }
        val current = _gameState.value
        if (current.status == GameStatus.READY) {
            val highScore = careerStats.value.highScoreByMode[updated.gameMode] ?: 0
            _gameState.value = createInitialGameForCurrentSettings(updated, highScore)
        } else if (!enabled) {
            _gameState.value = current.copy(
                foods = current.foods.filter { it.type != FoodType.POISON_SKULL }
            )
        } else if (current.foods.none { it.type == FoodType.POISON_SKULL }) {
            val spawned = engine.spawnPoisonSkull(current)
            if (spawned != null) {
                _gameState.value = current.copy(foods = current.foods + spawned)
            }
        }
    }

    fun togglePoisonEnabled() {
        setPoisonEnabled(!settings.value.poisonEnabled)
    }

    fun setBoardSize(size: BoardSize) {
        val updated = repository.updateSettings { it.copy(boardSize = size) }
        val (targetW, targetH) = resolveTargetGridDimensions(updated)
        if (_gameState.value.status == GameStatus.READY) {
            val highScore = careerStats.value.highScoreByMode[updated.gameMode] ?: 0
            _gameState.value = createInitialGameForCurrentSettings(updated, highScore)
        } else {
            _gameState.value = engine.resizeBoardPreservingState(
                state = _gameState.value,
                settings = updated,
                newWidth = targetW,
                newHeight = targetH
            )
        }
    }

    fun setFullAreaGrid(enabled: Boolean) {
        val updated = repository.updateSettings { it.copy(fullAreaGrid = enabled) }
        val (targetW, targetH) = resolveTargetGridDimensions(updated)
        _gameState.value = engine.resizeBoardPreservingState(
            state = _gameState.value,
            settings = updated,
            newWidth = targetW,
            newHeight = targetH
        )
    }

    fun toggleFullAreaGrid() {
        setFullAreaGrid(!settings.value.fullAreaGrid)
    }

    fun setLabyrinthLayout(layout: LabyrinthLayout) {
        val updated = repository.updateSettings { it.copy(labyrinthLayout = layout) }
        if (_gameState.value.status == GameStatus.READY) {
            val highScore = careerStats.value.highScoreByMode[updated.gameMode] ?: 0
            _gameState.value = createInitialGameForCurrentSettings(updated, highScore)
        }
    }

    fun setControlScheme(scheme: ControlScheme) {
        repository.updateSettings { it.copy(controlScheme = scheme) }
    }

    fun setSnakeSkin(skin: SnakeSkin) {
        repository.updateSettings { it.copy(snakeSkin = skin) }
    }

    fun setArenaTheme(theme: ArenaTheme) {
        repository.updateSettings { it.copy(arenaTheme = theme) }
    }

    fun setDynamicSpeedScaling(enabled: Boolean) {
        repository.updateSettings { it.copy(dynamicSpeedScaling = enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        repository.updateSettings { it.copy(soundEnabled = enabled) }
        if (enabled) {
            feedbackManager.onEatApple(2, soundEnabled = true, hapticsEnabled = false)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        repository.updateSettings { it.copy(hapticsEnabled = enabled) }
        if (enabled) {
            feedbackManager.onTurn(soundEnabled = false, hapticsEnabled = true)
        }
    }

    fun setShowGridLines(enabled: Boolean) {
        repository.updateSettings { it.copy(showGridLines = enabled) }
    }

    fun setSmoothMovement(enabled: Boolean) {
        repository.updateSettings { it.copy(smoothMovement = enabled) }
        if (!enabled) {
            _subTickFraction.value = 1f
        }
    }

    fun setSwipeSensitivity(value: Float) {
        repository.updateSettings { it.copy(swipeSensitivity = value.coerceIn(0.5f, 1.5f)) }
    }

    fun testAudioAndHaptics() {
        val s = settings.value
        feedbackManager.onEatGoldenOrPowerUp(s.soundEnabled, s.hapticsEnabled)
    }

    fun resetSettingsToDefaults() {
        val defaults = repository.resetSettingsToDefaults()
        val (targetW, targetH) = resolveTargetGridDimensions(defaults)
        if (_gameState.value.status == GameStatus.READY) {
            val highScore = careerStats.value.highScoreByMode[defaults.gameMode] ?: 0
            _gameState.value = createInitialGameForCurrentSettings(defaults, highScore)
        } else {
            _gameState.value = engine.resizeBoardPreservingState(
                state = _gameState.value,
                settings = defaults,
                newWidth = targetW,
                newHeight = targetH
            )
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearRunHistory()
        }
    }

    class Factory(
        private val repository: GameRepository,
        private val feedbackManager: ArcadeFeedbackManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameViewModel(repository, feedbackManager) as T
        }
    }
}
