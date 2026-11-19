package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_records")
data class RunRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val score: Int,
    val gameMode: String,
    val difficulty: String,
    val boardSize: String,
    val snakeSkin: String,
    val applesEaten: Int,
    val goldenEaten: Int,
    val powerUpsCollected: Int,
    val portalsUsed: Int,
    val maxCombo: Int,
    val finalLength: Int,
    val durationMillis: Long,
    val deathReason: String,
    val isVictory: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_game")
data class SavedGameEntity(
    @PrimaryKey val slotId: Int = 1,
    val modeName: String,
    val difficultyName: String,
    val boardWidth: Int,
    val boardHeight: Int,
    val labyrinthLayoutName: String,
    val snakeEncoded: String, // "x1,y1;x2,y2;..."
    val directionName: String,
    val obstaclesEncoded: String,
    val portalsEncoded: String, // "id,ax,ay,bx,by,color;..."
    val foodsEncoded: String, // "id,x,y,type,spawnTick,expTick;..."
    val buffsEncoded: String, // "type,remTicks,totTicks;..."
    val score: Int,
    val applesEaten: Int,
    val goldenEaten: Int,
    val powerUpsCollected: Int,
    val portalsUsed: Int,
    val comboCount: Int,
    val maxComboReached: Int,
    val ticksSinceLastEat: Int,
    val currentTick: Int,
    val currentTickIntervalMs: Long,
    val remainingBlitzMillis: Long,
    val elapsedPlayMillis: Long,
    val pendingGrowth: Int,
    val savedAtTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val currentValue: Int,
    val isUnlocked: Boolean,
    val unlockedAtTimestamp: Long? = null
)
