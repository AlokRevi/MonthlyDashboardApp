package com.alok.monthlydashboard.dto.dashboard;

import com.alok.monthlydashboard.common.enums.RecurrenceType;

import java.util.List;

public record TimelineTaskResponse(
        Long taskId,
        String taskName,
        RecurrenceType recurrenceType,
        String recurrenceSummary,
        List<TimelineOccurrenceBucketResponse> buckets
) {
}
