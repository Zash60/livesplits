package com.livesplits.data.local.dao

import androidx.room.*
import com.livesplits.data.local.entity.Game
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM games ORDER BY createdAt DESC")
    fun getAllGames(): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameById(id: Long): Game?

    @Query("SELECT * FROM games WHERE id = :id")
    fun getGameByIdFlow(id: Long): Flow<Game?>

    @Query("SELECT * FROM games WHERE name LIKE '%' || :query || '%'")
    fun searchGames(query: String): Flow<List<Game>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: Game): Long

    @Update
    suspend fun updateGame(game: Game)

    @Delete
    suspend fun deleteGame(game: Game)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGameById(id: Long)

    @Query("SELECT COUNT(*) FROM games")
    fun getGameCount(): Flow<Int>
}
