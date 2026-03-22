package com.livesplits.domain.usecase.category

import com.livesplits.data.local.dao.CategoryDao
import com.livesplits.data.local.entity.Category
import com.livesplits.domain.model.CategoryDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetCategoriesByGameIdUseCase @Inject constructor(
    private val categoryDao: CategoryDao
) {
    operator fun invoke(gameId: Long): Flow<List<CategoryDomain>> {
        return categoryDao.getCategoriesByGameId(gameId).map { categories ->
            categories.map { it.toDomain() }
        }
    }
}

class GetCategoryByIdUseCase @Inject constructor(
    private val categoryDao: CategoryDao
) {
    suspend operator fun invoke(id: Long): CategoryDomain? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }
}

class SearchCategoriesUseCase @Inject constructor(
    private val categoryDao: CategoryDao
) {
    operator fun invoke(gameId: Long, query: String): Flow<List<CategoryDomain>> {
        return categoryDao.searchCategories(gameId, query).map { categories ->
            categories.map { it.toDomain() }
        }
    }
}

class InsertCategoryUseCase @Inject constructor(
    private val categoryDao: CategoryDao
) {
    suspend operator fun invoke(
        gameId: Long,
        name: String,
        pbTimeMs: Long = 0L,
        runCount: Int = 0,
        speedrunCategoryId: String? = null
    ): Long {
        val category = Category(
            gameId = gameId,
            name = name,
            pbTimeMs = pbTimeMs,
            runCount = runCount,
            speedrunCategoryId = speedrunCategoryId
        )
        return categoryDao.insertCategory(category)
    }
}

class UpdateCategoryUseCase @Inject constructor(
    private val categoryDao: CategoryDao
) {
    suspend operator fun invoke(category: CategoryDomain) {
        val entity = category.toEntity()
        categoryDao.updateCategory(entity)
    }
}

class UpdateCategoryPbUseCase @Inject constructor(
    private val categoryDao: CategoryDao
) {
    suspend operator fun invoke(categoryId: Long, newPbTimeMs: Long) {
        val category = categoryDao.getCategoryById(categoryId) ?: return
        val updated = category.copy(pbTimeMs = newPbTimeMs)
        categoryDao.updateCategory(updated)
    }
}

class UpdateCategoryRunCountUseCase @Inject constructor(
    private val categoryDao: CategoryDao
) {
    suspend operator fun invoke(categoryId: Long, newRunCount: Int) {
        val category = categoryDao.getCategoryById(categoryId) ?: return
        val updated = category.copy(runCount = newRunCount)
        categoryDao.updateCategory(updated)
    }
}

class DeleteCategoryUseCase @Inject constructor(
    private val categoryDao: CategoryDao
) {
    suspend operator fun invoke(categoryId: Long) {
        categoryDao.deleteCategoryById(categoryId)
    }
}

// Extension functions for mapping
fun Category.toDomain(): CategoryDomain = CategoryDomain(
    id = id,
    gameId = gameId,
    name = name,
    pbTimeMs = pbTimeMs,
    runCount = runCount,
    speedrunCategoryId = speedrunCategoryId,
    createdAt = createdAt
)

fun CategoryDomain.toEntity(): Category = Category(
    id = id,
    gameId = gameId,
    name = name,
    pbTimeMs = pbTimeMs,
    runCount = runCount,
    speedrunCategoryId = speedrunCategoryId,
    createdAt = createdAt
)
