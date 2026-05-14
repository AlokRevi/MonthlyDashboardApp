package com.alok.monthlydashboard.dto.importsetup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SetupImportCategoryRequest(
        Long id,
        String name,
        String color,
        String requires,
        List<String> feelsLike,
        Integer taskCount
) {
}
