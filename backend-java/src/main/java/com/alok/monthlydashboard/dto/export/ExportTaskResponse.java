package com.alok.monthlydashboard.dto.export;

import com.alok.monthlydashboard.common.enums.RecurrenceType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExportTaskResponse(
        Long id,
        Long categoryId,
        String name,
        String description,
        RecurrenceType recurrenceType,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
