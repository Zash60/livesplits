package com.livesplits.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.livesplits.data.local.dao.CategoryDao
import com.livesplits.data.local.dao.GameDao
import com.livesplits.data.local.dao.SegmentDao
import com.livesplits.data.local.entity.Category
import com.livesplits.data.local.entity.Game
import com.livesplits.data.local.entity.Segment

@Database(
    entities = [
        Game::class,
        Category::class,
        Segment::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LiveSplitsDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao
    abstract fun categoryDao(): CategoryDao
    abstract fun segmentDao(): SegmentDao

    companion object {
        const val DATABASE_NAME = "livesplits_database"
    }
}
