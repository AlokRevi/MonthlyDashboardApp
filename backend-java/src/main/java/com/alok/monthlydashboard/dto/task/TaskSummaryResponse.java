package com.alok.monthlydashboard.dto.task;

import java.time.LocalDate;
import com.alok.monthlydashboard.common.enums.RecurrenceType;

public record TaskSummaryResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        RecurrenceType recurrenceType,
        LocalDate startDate,
        LocalDate endDate,
        boolean isActive
) {
}