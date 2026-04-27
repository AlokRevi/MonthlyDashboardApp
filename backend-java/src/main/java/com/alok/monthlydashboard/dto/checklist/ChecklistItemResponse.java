package com.alok.monthlydashboard.dto.checklist;
import com.alok.monthlydashboard.common.enums.OccurrenceStatus;

import java.time.LocalDate;

public record ChecklistItemResponse(
        Long taskId,
        String taskName,
        Long categoryId,
        String categoryName,
        LocalDate occurrenceDate,
        OccurrenceStatus status
) {
}