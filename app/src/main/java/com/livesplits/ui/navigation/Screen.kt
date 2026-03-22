package com.livesplits.ui.navigation

sealed class Screen(val route: String) {
    object GamesList : Screen("games_list")
    object Game : Screen("game/{gameId}") {
        fun createRoute(gameId: Long) = "game/$gameId"
    }
    object Splits : Screen("splits/{categoryId}") {
        fun createRoute(categoryId: Long) = "splits/$categoryId"
    }
    object Settings : Screen("settings")
}
