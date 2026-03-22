package com.livesplits.domain.usecase.segment

import com.livesplits.data.local.dao.SegmentDao
import com.livesplits.data.local.entity.Segment
import com.livesplits.domain.model.SegmentDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetSegmentsByCategoryIdUseCase @Inject constructor(
    private val segmentDao: SegmentDao
) {
    operator fun invoke(categoryId: Long): Flow<List<SegmentDomain>> {
        return segmentDao.getSegmentsByCategoryId(categoryId).map { segments ->
            segments.map { it.toDomain() }
        }
    }
}

class GetSegmentByIdUseCase @Inject constructor(
    private val segmentDao: SegmentDao
) {
    suspend operator fun invoke(id: Long): SegmentDomain? {
        return segmentDao.getSegmentById(id)?.toDomain()
    }
}

class InsertSegmentUseCase @Inject constructor(
    private val segmentDao: SegmentDao
) {
    suspend operator fun invoke(
        categoryId: Long,
        name: String,
        position: Int,
        pbTimeMs: Long = 0L,
        bestTimeMs: Long = 0L
    ): Long {
        val segment = Segment(
            categoryId = categoryId,
            name = name,
            position = position,
            pbTimeMs = pbTimeMs,
            bestTimeMs = bestTimeMs
        )
        return segmentDao.insertSegment(segment)
    }
}

class UpdateSegmentUseCase @Inject constructor(
    private val segmentDao: SegmentDao
) {
    suspend operator fun invoke(segment: SegmentDomain) {
        val entity = segment.toEntity()
        segmentDao.updateSegment(entity)
    }
}

class UpdateSegmentTimesUseCase @Inject constructor(
    private val segmentDao: SegmentDao
) {
    suspend operator fun invoke(
        segmentId: Long,
        pbTimeMs: Long? = null,
        bestTimeMs: Long? = null
    ) {
        val segment = segmentDao.getSegmentById(segmentId) ?: return
        val updated = segment.copy(
            pbTimeMs = pbTimeMs ?: segment.pbTimeMs,
            bestTimeMs = bestTimeMs ?: segment.bestTimeMs
        )
        segmentDao.updateSegment(updated)
    }
}

class DeleteSegmentUseCase @Inject constructor(
    private val segmentDao: SegmentDao
) {
    suspend operator fun invoke(segmentId: Long) {
        segmentDao.deleteSegmentById(segmentId)
    }
}

class ReorderSegmentUseCase @Inject constructor(
    private val segmentDao: SegmentDao
) {
    suspend operator fun invoke(categoryId: Long, fromPosition: Int, toPosition: Int) {
        segmentDao.reorderSegment(categoryId, fromPosition, toPosition)
    }
}

class GetTotalPbTimeUseCase @Inject constructor(
    private val segmentDao: SegmentDao
) {
    suspend operator fun invoke(categoryId: Long): Long {
        return segmentDao.getTotalPbTime(categoryId) ?: 0L
    }
}

class GetSumOfBestUseCase @Inject constructor(
    private val segmentDao: SegmentDao
) {
    suspend operator fun invoke(categoryId: Long): Long {
        return segmentDao.getSumOfBest(categoryId) ?: 0L
    }
}

// Extension functions for mapping
fun Segment.toDomain(): SegmentDomain = SegmentDomain(
    id = id,
    categoryId = categoryId,
    name = name,
    position = position,
    pbTimeMs = pbTimeMs,
    bestTimeMs = bestTimeMs,
    createdAt = createdAt
)

fun SegmentDomain.toEntity(): Segment = Segment(
    id = id,
    categoryId = categoryId,
    name = name,
    position = position,
    pbTimeMs = pbTimeMs,
    bestTimeMs = bestTimeMs,
    createdAt = createdAt
)
