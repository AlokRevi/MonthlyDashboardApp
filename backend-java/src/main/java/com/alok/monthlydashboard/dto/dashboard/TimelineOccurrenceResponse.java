package com.alok.monthlydashboard.dto.dashboard;

import com.alok.monthlydashboard.common.enums.OccurrenceStatus;

import java.time.LocalDate;

public record TimelineOccurrenceResponse(
        LocalDate occurrenceDate,
        boolean completed,
        LocalDate completionDate,
        OccurrenceStatus status
) {
}
