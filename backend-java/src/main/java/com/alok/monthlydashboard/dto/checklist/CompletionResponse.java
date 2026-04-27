package com.alok.monthlydashboard.dto.checklist;
import com.alok.monthlydashboard.common.enums.OccurrenceStatus;
import java.time.LocalDate;

public record CompletionResponse(
        Long taskId,
        LocalDate occurrenceDate,
        LocalDate completionDate,
        OccurrenceStatus status,
        String message
) {
}