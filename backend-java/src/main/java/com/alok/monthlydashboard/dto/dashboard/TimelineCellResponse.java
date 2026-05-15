package com.alok.monthlydashboard.dto.dashboard;

import com.alok.monthlydashboard.common.enums.TimelineCellType;

import java.time.LocalDate;

public record TimelineCellResponse(
        String key,
        LocalDate startDate,
        LocalDate endDate,
        String label,
        String secondaryLabel,
        TimelineCellType cellType,
        int segmentIndex,
        int continuedIndex,
        boolean isToday,
        boolean isWeekend,
        boolean isCurrentPeriod
) {
}
