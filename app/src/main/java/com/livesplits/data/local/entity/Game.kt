package com.livesplits.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a game in the user's collection.
 */
@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val packageName: String? = null, // For linking to installed apps
    val createdAt: Long = System.currentTimeMillis()
)
