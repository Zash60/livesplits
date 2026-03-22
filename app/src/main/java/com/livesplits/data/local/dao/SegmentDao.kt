package com.livesplits.data.local.dao

import androidx.room.*
import com.livesplits.data.local.entity.Segment
import kotlinx.coroutines.flow.Flow

@Dao
interface SegmentDao {

    @Query("SELECT * FROM segments WHERE categoryId = :categoryId ORDER BY position ASC")
    fun getSegmentsByCategoryId(categoryId: Long): Flow<List<Segment>>

    @Query("SELECT * FROM segments WHERE id = :id")
    suspend fun getSegmentById(id: Long): Segment?

    @Query("SELECT * FROM segments WHERE id = :id")
    fun getSegmentByIdFlow(id: Long): Flow<Segment?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegment(segment: Segment): Long

    @Update
    suspend fun updateSegment(segment: Segment)

    @Delete
    suspend fun deleteSegment(segment: Segment)

    @Query("DELETE FROM segments WHERE id = :id")
    suspend fun deleteSegmentById(id: Long)

    @Query("DELETE FROM segments WHERE categoryId = :categoryId")
    suspend fun deleteAllSegmentsByCategoryId(categoryId: Long)

    @Query("SELECT COUNT(*) FROM segments WHERE categoryId = :categoryId")
    fun getSegmentCount(categoryId: Long): Flow<Int>

    @Query("SELECT SUM(pbTimeMs) FROM segments WHERE categoryId = :categoryId")
    suspend fun getTotalPbTime(categoryId: Long): Long?

    @Query("SELECT SUM(bestTimeMs) FROM segments WHERE categoryId = :categoryId")
    suspend fun getSumOfBest(categoryId: Long): Long?

    @Query("UPDATE segments SET position = :newPosition WHERE id = :segmentId")
    suspend fun updateSegmentPosition(segmentId: Long, newPosition: Int)

    @Query("UPDATE segments SET position = position + 1 WHERE categoryId = :categoryId AND position >= :fromPosition AND position < :toPosition")
    suspend fun shiftSegmentsUp(categoryId: Long, fromPosition: Int, toPosition: Int)

    @Query("UPDATE segments SET position = position - 1 WHERE categoryId = :categoryId AND position <= :fromPosition AND position > :toPosition")
    suspend fun shiftSegmentsDown(categoryId: Long, fromPosition: Int, toPosition: Int)

    @Transaction
    suspend fun reorderSegment(categoryId: Long, fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            shiftSegmentsDown(categoryId, fromPosition, toPosition)
        } else if (fromPosition > toPosition) {
            shiftSegmentsUp(categoryId, fromPosition, toPosition)
        }
    }
}
