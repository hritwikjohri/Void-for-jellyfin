package com.hritwik.avoid.domain.mapper

import com.hritwik.avoid.data.remote.dto.activity.ActivityLogEntryDto
import com.hritwik.avoid.data.remote.dto.activity.ActivityLogEntryQueryResult
import com.hritwik.avoid.domain.model.activity.ActivityLogEntry
import com.hritwik.avoid.domain.model.activity.ActivityLogResult
import com.hritwik.avoid.domain.model.activity.ActivityType
import com.hritwik.avoid.domain.model.activity.LogSeverity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityLogMapper @Inject constructor() {

    fun mapActivityLogEntryDto(dto: ActivityLogEntryDto): ActivityLogEntry {
        return ActivityLogEntry(
            id = dto.id,
            name = dto.name,
            overview = dto.overview,
            shortOverview = dto.shortOverview,
            type = ActivityType.fromString(dto.type),
            itemId = dto.itemId,
            date = dto.date,
            userId = dto.userId,
            userPrimaryImageTag = dto.userPrimaryImageTag,
            severity = LogSeverity.fromString(dto.severity)
        )
    }

    fun mapActivityLogQueryResult(dto: ActivityLogEntryQueryResult): ActivityLogResult {
        return ActivityLogResult(
            items = dto.items.map { mapActivityLogEntryDto(it) },
            totalRecordCount = dto.totalRecordCount,
            startIndex = dto.startIndex
        )
    }
}
