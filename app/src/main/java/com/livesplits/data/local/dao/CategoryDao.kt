package com.livesplits.data.local.dao

import androidx.room.*
import com.livesplits.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE gameId = :gameId ORDER BY createdAt DESC")
    fun getCategoriesByGameId(gameId: Long): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategoryByIdFlow(id: Long): Flow<Category?>

    @Query("SELECT * FROM categories WHERE gameId = :gameId AND name LIKE '%' || :query || '%'")
    fun searchCategories(gameId: Long, query: String): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("DELETE FROM categories WHERE gameId = :gameId")
    suspend fun deleteAllCategoriesByGameId(gameId: Long)

    @Query("SELECT COUNT(*) FROM categories WHERE gameId = :gameId")
    fun getCategoryCount(gameId: Long): Flow<Int>
}
