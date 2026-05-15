package com.alok.monthlydashboard.dto.dashboard;

import java.util.List;

public record TimelineOccurrenceBucketResponse(
        String cellKey,
        int totalOccurrences,
        int completedOccurrences,
        String completionLabel,
        String collapsedLabel,
        List<TimelineOccurrenceResponse> occurrences
) {
}
