package com.alok.monthlydashboard.dto.importsetup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SetupImportRuleRequest(
        List<Integer> fixedDates,
        Boolean fallbackToLastDay,
        Integer intervalValue,
        String intervalUnit,
        String weekday,
        String weekOfMonth
) {
}
