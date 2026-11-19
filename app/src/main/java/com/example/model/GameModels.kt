package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

data class GridPoint(val x: Int, val y: Int) {
    fun step(direction: Direction): GridPoint = GridPoint(x + direction.dx, y + direction.dy)

    fun wrap(width: Int, height: Int): GridPoint = GridPoint(
        x = ((x % width) + width) % width,
        y = ((y % height) + height) % height
    )

    fun isInside(width: Int, height: Int): Boolean = x in 0 until width && y in 0 until height

    fun manhattanTo(other: GridPoint): Int = abs(x - other.x) + abs(y - other.y)

    fun chebyshevTo(other: GridPoint): Int = maxOf(abs(x - other.x), abs(y - other.y))
}

enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    fun isOpposite(other: Direction): Boolean = (dx + other.dx == 0) && (dy + other.dy == 0)

    fun turnLeft(): Direction = when (this) {
        UP -> LEFT
        LEFT -> DOWN
        DOWN -> RIGHT
        RIGHT -> UP
    }

    fun turnRight(): Direction = when (this) {
        UP -> RIGHT
        RIGHT -> DOWN
        DOWN -> LEFT
        LEFT -> UP
    }
}

enum class GameMode(
    val title: String,
    val subtitle: String,
    val description: String,
    val scoreMultiplier: Float,
    val wrapsWalls: Boolean,
    val hasObstacles: Boolean,
    val hasPortals: Boolean,
    val isTimedBlitz: Boolean
) {
    CLASSIC(
        title = "Classic Arena",
        subtitle = "Walled Perimeter",
        description = "Pure arcade survival. Avoid the outer containment walls and your own tail as your serpent accelerates.",
        scoreMultiplier = 1.0f,
        wrapsWalls = false,
        hasObstacles = false,
        hasPortals = false,
        isTimedBlitz = false
    ),
    ZEN(
        title = "Zen Hyperspace",
        subtitle = "Wrap-Around Edges",
        description = "No outer walls—glide seamlessly across opposing edges of the grid for flow-state high-length runs.",
        scoreMultiplier = 0.85f,
        wrapsWalls = true,
        hasObstacles = false,
        hasPortals = false,
        isTimedBlitz = false
    ),
    LABYRINTH(
        title = "Tactical Labyrinth",
        subtitle = "4 Obstacle Layouts",
        description = "Navigate fortified interior walls and tight corridors for a 1.5x tactical score bonus.",
        scoreMultiplier = 1.5f,
        wrapsWalls = false,
        hasObstacles = true,
        hasPortals = false,
        isTimedBlitz = false
    ),
    PORTAL_FLUX(
        title = "Portal Flux",
        subtitle = "Quantum Wormholes",
        description = "Paired dimensional portals warp your serpent across the arena instantly and grant +15 bonus Flux points.",
        scoreMultiplier = 1.3f,
        wrapsWalls = true,
        hasObstacles = false,
        hasPortals = true,
        isTimedBlitz = false
    ),
    TIME_BLITZ(
        title = "Time Blitz 120s",
        subtitle = "Overdrive Clock Rush",
        description = "120-second score attack! Eating orbs adds bonus seconds. Wall impacts trim your tail instead of ending the run.",
        scoreMultiplier = 1.4f,
        wrapsWalls = false,
        hasObstacles = false,
        hasPortals = true,
        isTimedBlitz = true
    )
}

enum class LabyrinthLayout(val id: Int, val title: String) {
    CROSSHAIR_PILLARS(0, "Crosshair Pillars"),
    TWIN_GATES(1, "Twin Bastion Gates"),
    CENTRAL_VAULT(2, "Split Vault Corridors"),
    CORNER_BUNKERS(3, "Quadrant Bunkers")
}

enum class Difficulty(
    val title: String,
    val baseTickMs: Long,
    val minTickMs: Long,
    val speedStepMs: Long,
    val scoreMultiplier: Float,
    val badgeText: String,
    val defaultPoisonEnabled: Boolean
) {
    CHILL(
        title = "Chill",
        baseTickMs = 185L,
        minTickMs = 115L,
        speedStepMs = 2L,
        scoreMultiplier = 0.8f,
        badgeText = "RELAXED",
        defaultPoisonEnabled = false
    ),
    ARCADE(
        title = "Arcade",
        baseTickMs = 135L,
        minTickMs = 75L,
        speedStepMs = 2L,
        scoreMultiplier = 1.0f,
        badgeText = "STANDARD",
        defaultPoisonEnabled = false
    ),
    HYPER(
        title = "Hyper",
        baseTickMs = 96L,
        minTickMs = 56L,
        speedStepMs = 2L,
        scoreMultiplier = 1.35f,
        badgeText = "FAST",
        defaultPoisonEnabled = true
    ),
    OVERCLOCKED(
        title = "Overclocked",
        baseTickMs = 70L,
        minTickMs = 46L,
        speedStepMs = 1L,
        scoreMultiplier = 1.75f,
        badgeText = "EXTREME",
        defaultPoisonEnabled = true
    )
}

enum class BoardSize(
    val title: String,
    val columns: Int,
    val rows: Int,
    val subtitle: String
) {
    COMPACT("Compact", 15, 15, "15×15 Tight CQC"),
    STANDARD("Standard", 20, 20, "20×20 Classic Balanced"),
    EXPANSIVE("Expansive", 24, 24, "24×24 Grand Tactical");

    companion object {
        fun fromDimensions(width: Int, height: Int): BoardSize {
            val minDim = minOf(width, height)
            return when {
                minDim <= 16 -> COMPACT
                minDim >= 23 -> EXPANSIVE
                else -> STANDARD
            }
        }
    }
}

enum class ControlScheme(
    val title: String,
    val shortLabel: String,
    val description: String
) {
    DPAD(
        title = "Tactical D-Pad + Swipe",
        shortLabel = "D-Pad",
        description = "Precision 4-way directional pad below the arena plus full-area swipe support."
    ),
    SPLIT_BUTTONS(
        title = "Pro Dual-Thumb Split",
        shortLabel = "Dual Split",
        description = "Left thumb steers Left/Right, Right thumb steers Up/Down for zero-travel arcade speed."
    ),
    RELATIVE_STEER(
        title = "2-Button Relative Turn",
        shortLabel = "2-Btn Turn",
        description = "Two oversized buttons: Turn Left (↺) and Turn Right (↻) relative to your serpent's heading."
    ),
    SWIPE_ONLY(
        title = "Pure Gesture Arena",
        shortLabel = "Swipe Only",
        description = "Hides bottom controls to maximize play area. Glide or swipe anywhere between header and bottom bar."
    )
}

enum class SnakeSkin(
    val title: String,
    val subtitle: String,
    val headColor: Color,
    val bodyStartColor: Color,
    val bodyEndColor: Color,
    val eyeColor: Color,
    val glowColor: Color
) {
    CYBER_EMERALD(
        title = "Cyber Emerald",
        subtitle = "Signature Serpentis",
        headColor = Color(0xFF00FFAA),
        bodyStartColor = Color(0xFF00E676),
        bodyEndColor = Color(0xFF00897B),
        eyeColor = Color(0xFF070B19),
        glowColor = Color(0xFF00F59B)
    ),
    SOLAR_FLARE(
        title = "Solar Flare",
        subtitle = "Molten Gold & Ignis",
        headColor = Color(0xFFFFEA00),
        bodyStartColor = Color(0xFFFF9100),
        bodyEndColor = Color(0xFFFF1744),
        eyeColor = Color(0xFF1A0505),
        glowColor = Color(0xFFFFAB00)
    ),
    PLASMA_VIOLET(
        title = "Plasma Violet",
        subtitle = "Synthwave Outrun",
        headColor = Color(0xFF00E5FF),
        bodyStartColor = Color(0xFFD500F9),
        bodyEndColor = Color(0xFF651FFF),
        eyeColor = Color(0xFF0A001A),
        glowColor = Color(0xFFE040FB)
    ),
    GLACIER_FROST(
        title = "Glacier Frost",
        subtitle = "Cryo Sub-Zero",
        headColor = Color(0xFFFFFFFF),
        bodyStartColor = Color(0xFF40C4FF),
        bodyEndColor = Color(0xFF0277BD),
        eyeColor = Color(0xFF001929),
        glowColor = Color(0xFF80D8FF)
    ),
    RETRO_NOKIA(
        title = "Matrix 3310",
        subtitle = "Classic Monochrome LCD",
        headColor = Color(0xFF1B2A0A),
        bodyStartColor = Color(0xFF263812),
        bodyEndColor = Color(0xFF394F1D),
        eyeColor = Color(0xFF9BBC0F),
        glowColor = Color(0xFF8BAC0F)
    ),
    ROYAL_OBSIDIAN(
        title = "Imperial Gold",
        subtitle = "Aurum & Deep Indigo",
        headColor = Color(0xFFFFD740),
        bodyStartColor = Color(0xFFB388FF),
        bodyEndColor = Color(0xFF311B92),
        eyeColor = Color(0xFF0B0719),
        glowColor = Color(0xFFFFD740)
    )
}

enum class ArenaTheme(
    val title: String,
    val subtitle: String,
    val boardBgStart: Color,
    val boardBgEnd: Color,
    val gridLineColor: Color,
    val wallColor: Color,
    val obstacleColor: Color,
    val isRetroLcd: Boolean = false
) {
    OBSIDIAN_GRID(
        title = "Obsidian Grid",
        subtitle = "Deep Cyber Navy",
        boardBgStart = Color(0xFF091024),
        boardBgEnd = Color(0xFF060A17),
        gridLineColor = Color(0xFF162447),
        wallColor = Color(0xFF00E5FF),
        obstacleColor = Color(0xFF334155)
    ),
    NOKIA_LCD(
        title = "Retro 1998 LCD",
        subtitle = "Backlit Olive Pixel Matrix",
        boardBgStart = Color(0xFF9BBC0F),
        boardBgEnd = Color(0xFF8BAC0F),
        gridLineColor = Color(0xFF7B9B0C),
        wallColor = Color(0xFF0F380F),
        obstacleColor = Color(0xFF306230),
        isRetroLcd = true
    ),
    SYNTHWAVE_DUSK(
        title = "Synthwave Dusk",
        subtitle = "Horizon Magenta & Violet",
        boardBgStart = Color(0xFF190A28),
        boardBgEnd = Color(0xFF0D0417),
        gridLineColor = Color(0xFF321450),
        wallColor = Color(0xFFFF2A85),
        obstacleColor = Color(0xFF4A1E75)
    ),
    CARBON_TACTICAL(
        title = "Carbon Tactical",
        subtitle = "Matte Stealth Gunmetal",
        boardBgStart = Color(0xFF12151A),
        boardBgEnd = Color(0xFF0B0D10),
        gridLineColor = Color(0xFF222730),
        wallColor = Color(0xFF00F59B),
        obstacleColor = Color(0xFF3F4756)
    )
}

enum class FoodType(
    val title: String,
    val basePoints: Int,
    val growthAmount: Int,
    val color: Color,
    val lifespanTicks: Int?,
    val buffDurationTicks: Int,
    val symbol: String,
    val isHazard: Boolean = false
) {
    CYBER_APPLE(
        title = "Cyber Apple",
        basePoints = 10,
        growthAmount = 1,
        color = Color(0xFFFF2A6D),
        lifespanTicks = null,
        buffDurationTicks = 0,
        symbol = "●",
        isHazard = false
    ),
    GOLDEN_CORE(
        title = "Golden Core",
        basePoints = 50,
        growthAmount = 2,
        color = Color(0xFFFFC837),
        lifespanTicks = 55,
        buffDurationTicks = 0,
        symbol = "★",
        isHazard = false
    ),
    MAGNET_CORE(
        title = "Flux Magnet",
        basePoints = 30,
        growthAmount = 1,
        color = Color(0xFF38BDF8),
        lifespanTicks = 60,
        buffDurationTicks = 55,
        symbol = "🧲",
        isHazard = false
    ),
    CHRONO_SHARD(
        title = "Chrono Slow-Mo",
        basePoints = 25,
        growthAmount = 1,
        color = Color(0xFF00E5FF),
        lifespanTicks = 65,
        buffDurationTicks = 55,
        symbol = "⧗",
        isHazard = false
    ),
    PHASE_GHOST(
        title = "Phase Ghost",
        basePoints = 30,
        growthAmount = 1,
        color = Color(0xFFB565FF),
        lifespanTicks = 60,
        buffDurationTicks = 50,
        symbol = "◈",
        isHazard = false
    ),
    SCORE_OVERDRIVE(
        title = "2x Overdrive",
        basePoints = 25,
        growthAmount = 1,
        color = Color(0xFF00F59B),
        lifespanTicks = 60,
        buffDurationTicks = 65,
        symbol = "⚡",
        isHazard = false
    ),
    SHRINK_CAPSULE(
        title = "Tail Trimmer",
        basePoints = 35,
        growthAmount = -3,
        color = Color(0xFFFF8C42),
        lifespanTicks = 55,
        buffDurationTicks = 0,
        symbol = "✂",
        isHazard = false
    ),
    POISON_SKULL(
        title = "Poison Skull",
        basePoints = 0,
        growthAmount = 0,
        color = Color(0xFFA3E635),
        lifespanTicks = 45,
        buffDurationTicks = 0,
        symbol = "☠",
        isHazard = true
    )
}

data class SpawnedFood(
    val id: Long,
    val position: GridPoint,
    val type: FoodType,
    val spawnedAtTick: Int,
    val expiresAtTick: Int?
) {
    fun remainingFraction(currentTick: Int): Float {
        val expires = expiresAtTick ?: return 1f
        val total = (expires - spawnedAtTick).coerceAtLeast(1)
        val left = (expires - currentTick).coerceAtLeast(0)
        return (left.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }
}

data class PortalPair(
    val id: Int,
    val entryA: GridPoint,
    val entryB: GridPoint,
    val colorHex: Long
)

data class ActiveBuff(
    val type: FoodType,
    val remainingTicks: Int,
    val totalTicks: Int
) {
    val progress: Float
        get() = (remainingTicks.toFloat() / totalTicks.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
}

data class ParticleBurst(
    val id: Long,
    val gridX: Float,
    val gridY: Float,
    val color: Color,
    val createdAtTick: Int
)

enum class GameStatus {
    READY,
    RUNNING,
    PAUSED,
    GAME_OVER,
    VICTORY,
    TIME_UP
}

enum class DeathReason(val message: String) {
    NONE(""),
    WALL_COLLISION("Perimeter Containment Wall Impact"),
    SELF_COLLISION("Self-Body Tail Collision"),
    OBSTACLE_COLLISION("Labyrinth Fortification Impact"),
    POISON_CONSUMED("Ingested a Toxic Poison Skull"),
    TIME_EXPIRED("120s Blitz Clock Expired"),
    BOARD_CLEARED("100% Grid Capacity Reached!")
}

data class GameSettings(
    val gameMode: GameMode = GameMode.CLASSIC,
    val difficulty: Difficulty = Difficulty.ARCADE,
    val boardSize: BoardSize = BoardSize.STANDARD,
    val fullAreaGrid: Boolean = false,
    val poisonEnabled: Boolean = false,
    val controlScheme: ControlScheme = ControlScheme.DPAD,
    val snakeSkin: SnakeSkin = SnakeSkin.CYBER_EMERALD,
    val arenaTheme: ArenaTheme = ArenaTheme.OBSIDIAN_GRID,
    val labyrinthLayout: LabyrinthLayout = LabyrinthLayout.CROSSHAIR_PILLARS,
    val dynamicSpeedScaling: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val showGridLines: Boolean = true,
    val smoothMovement: Boolean = true,
    val swipeSensitivity: Float = 1.0f
)

data class GameState(
    val mode: GameMode = GameMode.CLASSIC,
    val difficulty: Difficulty = Difficulty.ARCADE,
    val boardWidth: Int = 20,
    val boardHeight: Int = 20,
    val labyrinthLayout: LabyrinthLayout = LabyrinthLayout.CROSSHAIR_PILLARS,
    val snake: List<GridPoint> = emptyList(),
    val previousSnake: List<GridPoint> = emptyList(),
    val direction: Direction = Direction.RIGHT,
    val queuedDirections: List<Direction> = emptyList(),
    val obstacles: Set<GridPoint> = emptySet(),
    val portals: List<PortalPair> = emptyList(),
    val foods: List<SpawnedFood> = emptyList(),
    val activeBuffs: List<ActiveBuff> = emptyList(),
    val recentBursts: List<ParticleBurst> = emptyList(),
    val lastPoisonPosition: GridPoint? = null,
    val nextPoisonSpawnTick: Int = 0,
    val score: Int = 0,
    val initialHighScoreForMode: Int = 0,
    val highScoreForMode: Int = 0,
    val applesEaten: Int = 0,
    val goldenEaten: Int = 0,
    val powerUpsCollected: Int = 0,
    val portalsUsed: Int = 0,
    val comboCount: Int = 1,
    val maxComboReached: Int = 1,
    val ticksSinceLastEat: Int = 0,
    val comboWindowTicks: Int = 18,
    val currentTick: Int = 0,
    val currentTickIntervalMs: Long = 135L,
    val remainingBlitzMillis: Long = 120_000L,
    val elapsedPlayMillis: Long = 0L,
    val pendingGrowth: Int = 0,
    val status: GameStatus = GameStatus.READY,
    val deathReason: DeathReason = DeathReason.NONE,
    val isNewHighScore: Boolean = false
) {
    val isPhaseGhostActive: Boolean
        get() = activeBuffs.any { it.type == FoodType.PHASE_GHOST && it.remainingTicks > 0 }

    val isSlowMoActive: Boolean
        get() = activeBuffs.any { it.type == FoodType.CHRONO_SHARD && it.remainingTicks > 0 }

    val isOverdriveActive: Boolean
        get() = activeBuffs.any { it.type == FoodType.SCORE_OVERDRIVE && it.remainingTicks > 0 }

    val isMagnetActive: Boolean
        get() = activeBuffs.any { it.type == FoodType.MAGNET_CORE && it.remainingTicks > 0 }

    val snakeLength: Int
        get() = snake.size
}

data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val targetValue: Int,
    val badgeSymbol: String
)

object AchievementCatalog {
    val ALL = listOf(
        AchievementDef(
            id = "first_byte",
            title = "First Byte",
            description = "Consume your first 10 Cyber Apples across any arena.",
            targetValue = 10,
            badgeSymbol = "●"
        ),
        AchievementDef(
            id = "combo_striker",
            title = "Chain Reactor",
            description = "Reach a 5x Rapid Consumption Combo in a single run.",
            targetValue = 5,
            badgeSymbol = "⚡"
        ),
        AchievementDef(
            id = "golden_hunter",
            title = "Aurum Collector",
            description = "Capture 10 timed Golden Cores before they vanish.",
            targetValue = 10,
            badgeSymbol = "★"
        ),
        AchievementDef(
            id = "serpent_centurion",
            title = "Mega Serpent",
            description = "Grow your serpent to a length of 35 segments in one game.",
            targetValue = 35,
            badgeSymbol = "🐍"
        ),
        AchievementDef(
            id = "labyrinth_master",
            title = "Maze Tactician",
            description = "Score at least 500 points in Tactical Labyrinth mode.",
            targetValue = 500,
            badgeSymbol = "❖"
        ),
        AchievementDef(
            id = "portal_jumper",
            title = "Quantum Navigator",
            description = "Warp through dimensional portals 15 times.",
            targetValue = 15,
            badgeSymbol = "◎"
        ),
        AchievementDef(
            id = "hyper_survivor",
            title = "Overclocked Reflex",
            description = "Score 800+ points on Hyper or Overclocked speed.",
            targetValue = 800,
            badgeSymbol = "🔥"
        ),
        AchievementDef(
            id = "blitz_sprinter",
            title = "Blitz Champion",
            description = "Score 1,000+ points in Time Blitz 120s mode.",
            targetValue = 1000,
            badgeSymbol = "⏱"
        ),
        AchievementDef(
            id = "arcade_veteran",
            title = "Arcade Veteran",
            description = "Complete 15 total arena runs across all game modes.",
            targetValue = 15,
            badgeSymbol = "🏆"
        ),
        AchievementDef(
            id = "high_roller",
            title = "Apex Predator",
            description = "Achieve a single-run score of 2,000 points or higher.",
            targetValue = 2000,
            badgeSymbol = "👑"
        )
    )
}
