package com.livesplits.di

import android.content.Context
import androidx.room.Room
import com.livesplits.data.local.LiveSplitsDatabase
import com.livesplits.data.local.dao.CategoryDao
import com.livesplits.data.local.dao.GameDao
import com.livesplits.data.local.dao.SegmentDao
import com.livesplits.data.settings.SettingsRepository
import com.livesplits.network.SpeedrunRepository
import com.livesplits.network.SplitsIoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LiveSplitsDatabase {
        return Room.databaseBuilder(
            context,
            LiveSplitsDatabase::class.java,
            LiveSplitsDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideGameDao(database: LiveSplitsDatabase): GameDao {
        return database.gameDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: LiveSplitsDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideSegmentDao(database: LiveSplitsDatabase): SegmentDao {
        return database.segmentDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepository(context)
    }

    @Provides
    @Singleton
    fun provideSpeedrunRepository(): SpeedrunRepository {
        return SpeedrunRepository()
    }

    @Provides
    @Singleton
    fun provideSplitsIoRepository(): SplitsIoRepository {
        return SplitsIoRepository()
    }
}
