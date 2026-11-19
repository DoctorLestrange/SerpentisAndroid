package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.GameRepository
import com.example.engine.ArcadeFeedbackManager
import com.example.model.Direction
import com.example.ui.AppDestination
import com.example.ui.GameViewModel
import com.example.ui.screens.ArenaScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RecordsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WorkshopScreen
import com.example.ui.theme.SerpentisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SerpentisTheme {
                SerpentisApp()
            }
        }
    }
}

private data class NavTabItem(
    val destination: AppDestination,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val NAV_ITEMS = listOf(
    NavTabItem(
        destination = AppDestination.HOME,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_tab_home"
    ),
    NavTabItem(
        destination = AppDestination.ARENA,
        labelRes = R.string.nav_play,
        selectedIcon = Icons.Filled.PlayCircleFilled,
        unselectedIcon = Icons.Outlined.PlayCircleOutline,
        testTag = "nav_tab_arena"
    ),
    NavTabItem(
        destination = AppDestination.WORKSHOP,
        labelRes = R.string.nav_customize,
        selectedIcon = Icons.Filled.Palette,
        unselectedIcon = Icons.Outlined.Palette,
        testTag = "nav_tab_workshop"
    ),
    NavTabItem(
        destination = AppDestination.RECORDS,
        labelRes = R.string.nav_stats,
        selectedIcon = Icons.Filled.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents,
        testTag = "nav_tab_records"
    ),
    NavTabItem(
        destination = AppDestination.SETTINGS,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    )
)

@Composable
fun SerpentisApp(
    viewModel: GameViewModel = rememberDefaultGameViewModel()
) {
    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val subTickFraction by viewModel.subTickFraction.collectAsStateWithLifecycle()
    val runsByScore by viewModel.runsByScore.collectAsStateWithLifecycle()
    val careerStats by viewModel.careerStats.collectAsStateWithLifecycle()
    val savedGame by viewModel.savedGame.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val selectedModeFilter by viewModel.selectedRecordsModeFilter.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.pauseGame()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(currentDestination) {
        runCatching { focusRequester.requestFocus() }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && currentDestination == AppDestination.ARENA) {
                    when (event.key) {
                        Key.DirectionUp, Key.W -> {
                            viewModel.onDirectionInput(Direction.UP)
                            true
                        }
                        Key.DirectionDown, Key.S -> {
                            viewModel.onDirectionInput(Direction.DOWN)
                            true
                        }
                        Key.DirectionLeft, Key.A -> {
                            viewModel.onDirectionInput(Direction.LEFT)
                            true
                        }
                        Key.DirectionRight, Key.D -> {
                            viewModel.onDirectionInput(Direction.RIGHT)
                            true
                        }
                        Key.Spacebar, Key.P -> {
                            viewModel.togglePauseResume()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NAV_ITEMS.forEach { item ->
                            val selected = currentDestination == item.destination
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(item.destination) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NAV_ITEMS.forEach { item ->
                            val selected = currentDestination == item.destination
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(item.destination) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentDestination) {
                        AppDestination.HOME -> HomeScreen(
                            settings = settings,
                            careerStats = careerStats,
                            savedGame = savedGame,
                            onSelectMode = viewModel::setGameMode,
                            onSelectDifficulty = viewModel::setDifficulty,
                            onSelectLabyrinthLayout = viewModel::setLabyrinthLayout,
                            onSelectControlScheme = viewModel::setControlScheme,
                            onSetFullAreaGrid = viewModel::setFullAreaGrid,
                            onSetPoisonEnabled = viewModel::setPoisonEnabled,
                            onLaunchArena = { viewModel.launchNewRun(autoStart = false) },
                            onResumeSavedGame = { viewModel.resumeFromSavedEntity() },
                            onDiscardSavedGame = viewModel::discardSavedGame
                        )
                        AppDestination.ARENA -> ArenaScreen(
                            gameState = gameState,
                            settings = settings,
                            subTickFraction = subTickFraction,
                            onDirectionInput = viewModel::onDirectionInput,
                            onRelativeTurnInput = viewModel::onRelativeTurnInput,
                            onStartOrResume = viewModel::startOrResumeGame,
                            onTogglePause = viewModel::togglePauseResume,
                            onRestartGame = {
                                viewModel.prepareFreshArena(clearExistingSave = true)
                                viewModel.startOrResumeGame()
                            },
                            onToggleFullAreaGrid = viewModel::toggleFullAreaGrid,
                            onPlayAreaSizeChanged = viewModel::onPlayAreaSizeChanged,
                            onExitToHome = { viewModel.navigateTo(AppDestination.HOME) }
                        )
                        AppDestination.WORKSHOP -> WorkshopScreen(
                            settings = settings,
                            onSelectSkin = viewModel::setSnakeSkin,
                            onSelectArenaTheme = viewModel::setArenaTheme,
                            onSelectBoardSize = viewModel::setBoardSize,
                            onSetFullAreaGrid = viewModel::setFullAreaGrid,
                            onBack = { viewModel.navigateTo(AppDestination.HOME) }
                        )
                        AppDestination.RECORDS -> RecordsScreen(
                            runsByScore = runsByScore,
                            careerStats = careerStats,
                            achievements = achievements,
                            selectedModeFilter = selectedModeFilter,
                            onSelectModeFilter = viewModel::setRecordsModeFilter,
                            onClearHistory = viewModel::clearAllHistory,
                            onBack = { viewModel.navigateTo(AppDestination.HOME) }
                        )
                        AppDestination.SETTINGS -> SettingsScreen(
                            settings = settings,
                            onSelectControlScheme = viewModel::setControlScheme,
                            onSetSwipeSensitivity = viewModel::setSwipeSensitivity,
                            onSetDynamicSpeedScaling = viewModel::setDynamicSpeedScaling,
                            onSetSmoothMovement = viewModel::setSmoothMovement,
                            onSetShowGridLines = viewModel::setShowGridLines,
                            onSetFullAreaGrid = viewModel::setFullAreaGrid,
                            onSetPoisonEnabled = viewModel::setPoisonEnabled,
                            onSetSoundEnabled = viewModel::setSoundEnabled,
                            onSetHapticsEnabled = viewModel::setHapticsEnabled,
                            onTestFeedback = viewModel::testAudioAndHaptics,
                            onResetDefaults = viewModel::resetSettingsToDefaults,
                            onBack = { viewModel.navigateTo(AppDestination.HOME) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberDefaultGameViewModel(): GameViewModel {
    val context = LocalContext.current
    val factory = remember(context) {
        val db = AppDatabase.getInstance(context)
        val repo = GameRepository(db.gameDao(), context)
        val feedback = ArcadeFeedbackManager(context)
        GameViewModel.Factory(repo, feedback)
    }
    return viewModel(factory = factory)
}
