package com.alok.monthlydashboard.dto.export;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExportCompletionResponse(
        Long id,
        Long taskId,
        LocalDate occurrenceDate,
        LocalDate completionDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
