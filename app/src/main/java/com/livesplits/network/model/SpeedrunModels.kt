package com.livesplits.network.model

import com.google.gson.annotations.SerializedName

/**
 * Response models for speedrun.com API v1
 */

data class SpeedrunGamesResponse(
    val data: List<SpeedrunGame>
)

data class SpeedrunGame(
    val id: String,
    val names: Names,
    val abbreviation: String?
)

data class Names(
    val international: String
)

data class SpeedrunCategoriesResponse(
    val data: List<SpeedrunCategory>
)

data class SpeedrunCategory(
    val id: String,
    val name: String,
    @SerializedName("type")
    val categoryType: CategoryType
)

data class CategoryType(
    val type: String // "per-game" or "global"
)

data class SpeedrunLeaderboardResponse(
    val data: LeaderboardData
)

data class LeaderboardData(
    val runs: List<LeaderboardRun>
)

data class LeaderboardRun(
    val run: RunData,
    val place: Int
)

data class RunData(
    val times: Times,
    val date: String?,
    val players: List<PlayerRef>
)

data class Times(
    @SerializedName("primary_t")
    val primaryTime: Double // seconds as double
)

data class PlayerRef(
    val player: PlayerInfo
)

data class PlayerInfo(
    val name: String
)
