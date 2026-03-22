package com.livesplits.network

import com.livesplits.domain.model.LeaderboardEntry
import com.livesplits.domain.model.SpeedrunCategorySuggestion
import com.livesplits.domain.model.SpeedrunGameSuggestion
import com.livesplits.network.api.SpeedrunApi
import com.livesplits.network.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for speedrun.com API interactions.
 */
@Singleton
class SpeedrunRepository @Inject constructor() {

    private val api: SpeedrunApi

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.speedrun.com/api/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(SpeedrunApi::class.java)
    }

    /**
     * Search for games by name.
     * Returns a list of game suggestions.
     */
    suspend fun searchGames(query: String): Result<List<SpeedrunGameSuggestion>> {
        return try {
            val response = api.searchGames(query)
            if (response.isSuccessful) {
                val games = response.body()?.data?.map { game ->
                    SpeedrunGameSuggestion(
                        id = game.id,
                        name = game.names.international,
                        abbreviation = game.abbreviation
                    )
                } ?: emptyList()
                Result.success(games)
            } else {
                Result.failure(Exception("Failed to search games: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get categories for a specific game.
     * Returns a list of category suggestions.
     */
    suspend fun getCategories(gameId: String): Result<List<SpeedrunCategorySuggestion>> {
        return try {
            val response = api.getGameCategories(gameId)
            if (response.isSuccessful) {
                val categories = response.body()?.data?.map { category ->
                    SpeedrunCategorySuggestion(
                        id = category.id,
                        name = category.name,
                        type = category.categoryType.type
                    )
                } ?: emptyList()
                Result.success(categories)
            } else {
                Result.failure(Exception("Failed to get categories: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get leaderboard for a game category.
     * Returns a list of leaderboard entries.
     */
    suspend fun getLeaderboard(
        gameId: String,
        categoryId: String
    ): Result<List<LeaderboardEntry>> {
        return try {
            val response = api.getLeaderboard(gameId, categoryId)
            if (response.isSuccessful) {
                val runs = response.body()?.data?.runs ?: emptyList()
                val entries = runs.map { run ->
                    LeaderboardEntry(
                        rank = run.place,
                        playerName = run.run.players.firstOrNull()?.player?.name ?: "Unknown",
                        timeMs = (run.run.times.primaryTime * 1000).toLong(),
                        date = run.run.date?.let { parseIsoDate(it) }
                    )
                }
                Result.success(entries)
            } else {
                Result.failure(Exception("Failed to get leaderboard: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseIsoDate(dateString: String): Long {
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.getDefault())
            format.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
