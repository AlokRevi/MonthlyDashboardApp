package com.alok.monthlydashboard.dto.export;

import com.alok.monthlydashboard.entity.enums.IntervalUnit;
import com.alok.monthlydashboard.entity.enums.WeekOfMonth;
import com.alok.monthlydashboard.entity.enums.Weekday;

import java.time.LocalDateTime;
import java.util.List;

public record ExportRecurrenceRuleResponse(
        Long id,
        Long taskId,
        Integer intervalValue,
        IntervalUnit intervalUnit,
        Weekday weekday,
        WeekOfMonth weekOfMonth,
        Boolean fallbackToLastDay,
        List<ExportFixedDateResponse> fixedDates,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
