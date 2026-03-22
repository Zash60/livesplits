package com.livesplits.domain.usecase.game

import com.livesplits.data.local.dao.GameDao
import com.livesplits.data.local.entity.Game
import com.livesplits.domain.model.GameDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetGamesUseCase @Inject constructor(
    private val gameDao: GameDao
) {
    operator fun invoke(): Flow<List<GameDomain>> {
        return gameDao.getAllGames().map { games ->
            games.map { it.toDomain() }
        }
    }
}

class GetGameByIdUseCase @Inject constructor(
    private val gameDao: GameDao
) {
    suspend operator fun invoke(id: Long): GameDomain? {
        return gameDao.getGameById(id)?.toDomain()
    }
}

class SearchGamesUseCase @Inject constructor(
    private val gameDao: GameDao
) {
    operator fun invoke(query: String): Flow<List<GameDomain>> {
        return gameDao.searchGames(query).map { games ->
            games.map { it.toDomain() }
        }
    }
}

class InsertGameUseCase @Inject constructor(
    private val gameDao: GameDao
) {
    suspend operator fun invoke(name: String, packageName: String? = null, speedrunGameId: String? = null): Long {
        val game = Game(name = name, packageName = packageName, speedrunGameId = speedrunGameId)
        return gameDao.insertGame(game)
    }
}

class UpdateGameUseCase @Inject constructor(
    private val gameDao: GameDao
) {
    suspend operator fun invoke(game: GameDomain) {
        val entity = game.toEntity()
        gameDao.updateGame(entity)
    }
}

class DeleteGameUseCase @Inject constructor(
    private val gameDao: GameDao
) {
    suspend operator fun invoke(gameId: Long) {
        gameDao.deleteGameById(gameId)
    }
}

// Extension functions for mapping between entity and domain
fun Game.toDomain(): GameDomain = GameDomain(
    id = id,
    name = name,
    packageName = packageName,
    speedrunGameId = speedrunGameId,
    createdAt = createdAt
)

fun GameDomain.toEntity(): Game = Game(
    id = id,
    name = name,
    packageName = packageName,
    speedrunGameId = speedrunGameId,
    createdAt = createdAt
)
