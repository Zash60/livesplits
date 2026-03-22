package com.livesplits.domain.model

/**
 * Domain model for Game - represents the business entity.
 */
data class GameDomain(
    val id: Long,
    val name: String,
    val packageName: String?,
    val createdAt: Long
)

/**
 * Domain model for Category.
 */
data class CategoryDomain(
    val id: Long,
    val gameId: Long,
    val name: String,
    val pbTimeMs: Long,
    val runCount: Int,
    val speedrunCategoryId: String?,
    val createdAt: Long
)

/**
 * Domain model for Segment with calculated total time.
 */
data class SegmentDomain(
    val id: Long,
    val categoryId: Long,
    val name: String,
    val position: Int,
    val pbTimeMs: Long,
    val bestTimeMs: Long,
    val createdAt: Long
)

/**
 * Represents a leaderboard entry from speedrun.com.
 */
data class LeaderboardEntry(
    val rank: Int,
    val playerName: String,
    val timeMs: Long,
    val date: Long?
)

/**
 * Represents a game suggestion from speedrun.com API.
 */
data class SpeedrunGameSuggestion(
    val id: String,
    val name: String,
    val abbreviation: String?
)

/**
 * Represents a category suggestion from speedrun.com API.
 */
data class SpeedrunCategorySuggestion(
    val id: String,
    val name: String,
    val type: String
)

/**
 * Timer state for the speedrun timer.
 */
data class TimerState(
    val isRunning: Boolean,
    val currentTimeMs: Long,
    val currentSplitIndex: Int,
    val totalSplits: Int,
    val deltaMs: Long?, // Difference from PB
    val isAhead: Boolean?, // True if ahead of PB
    val isFinished: Boolean
)

/**
 * Comparison mode for timer.
 */
enum class ComparisonMode {
    PB,
    BEST_SEGMENTS
}

/**
 * Result of a timer operation.
 */
sealed class TimerResult {
    object Started : TimerResult()
    object Split : TimerResult()
    object Stopped : TimerResult()
    object Reset : TimerResult()
    data class SavedPB(val newTimeMs: Long) : TimerResult()
}
