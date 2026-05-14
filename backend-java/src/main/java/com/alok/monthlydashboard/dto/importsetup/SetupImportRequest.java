package com.alok.monthlydashboard.dto.importsetup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SetupImportRequest(
        String exportedAt,
        String version,
        List<SetupImportCategoryRequest> categories,
        List<SetupImportTaskRequest> tasks
) {
}
