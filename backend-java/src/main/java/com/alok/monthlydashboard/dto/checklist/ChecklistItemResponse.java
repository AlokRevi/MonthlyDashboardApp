package com.alok.monthlydashboard.dto.checklist;
import com.alok.monthlydashboard.common.enums.CategoryRequires;
import com.alok.monthlydashboard.common.enums.FeelsLikeLabel;
import com.alok.monthlydashboard.common.enums.OccurrenceStatus;

import java.time.LocalDate;
import java.util.List;

public record ChecklistItemResponse(
        Long taskId,
        String taskName,
        Long categoryId,
        String categoryName,
        String categoryColor,
        CategoryRequires categoryRequires,
        List<FeelsLikeLabel> categoryFeelsLike,
        LocalDate occurrenceDate,
        OccurrenceStatus status
) {
}
