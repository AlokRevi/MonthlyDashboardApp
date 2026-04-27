package com.alok.monthlydashboard.dto.task;

import java.util.List;
import com.alok.monthlydashboard.common.enums.*;

public record TaskRuleRequest(
        List<Integer> fixedDates,
        Integer intervalValue,
        IntervalUnit intervalUnit,
        Weekday weekday,
        WeekOfMonth weekOfMonth,
        Boolean fallbackToLastDay
) {
}