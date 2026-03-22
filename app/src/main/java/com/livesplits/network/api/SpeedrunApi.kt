package com.livesplits.network.api

import com.livesplits.network.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface for speedrun.com
 */
interface SpeedrunApi {

    /**
     * Search for games by name
     * GET /games?name={query}
     */
    @GET("games")
    suspend fun searchGames(
        @Query("name") name: String,
        @Query("max") limit: Int = 10
    ): Response<SpeedrunGamesResponse>

    /**
     * Get categories for a specific game
     * GET /games/{gameId}/categories
     */
    @GET("games/{gameId}/categories")
    suspend fun getGameCategories(
        @Path("gameId") gameId: String
    ): Response<SpeedrunCategoriesResponse>

    /**
     * Get leaderboard for a game category
     * GET /leaderboards/{gameId}/category/{categoryId}
     */
    @GET("leaderboards/{gameId}/category/{categoryId}")
    suspend fun getLeaderboard(
        @Path("gameId") gameId: String,
        @Path("categoryId") categoryId: String,
        @Query("top") top: Int = 100
    ): Response<SpeedrunLeaderboardResponse>
}
