package com.alok.monthlydashboard.dto.dashboard;

import java.util.List;

public record TimelineCategoryResponse(
        Long categoryId,
        String categoryName,
        String categoryColor,
        List<TimelineTaskResponse> tasks
) {
}
