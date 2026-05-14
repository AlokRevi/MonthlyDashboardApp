package com.alok.monthlydashboard.dto.importsetup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SetupImportTaskRequest(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String description,
        String recurrenceType,
        String startDate,
        String endDate,
        Boolean isActive,
        String energyOverride,
        String enjoymentOverride,
        String pressureOverride,
        String effortOverride,
        SetupImportRuleRequest rule
) {
}
