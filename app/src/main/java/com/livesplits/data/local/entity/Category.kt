package com.livesplits.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a category for a specific game.
 */
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["gameId"])]
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val name: String,
    val pbTimeMs: Long = 0L,
    val runCount: Int = 0,
    val speedrunCategoryId: String? = null, // For API integration
    val createdAt: Long = System.currentTimeMillis()
)
