package com.alok.monthlydashboard.dto.export;

import java.util.List;

public record SystemExportResponse(
        List<ExportCategoryResponse> categories,
        List<ExportTaskResponse> tasks,
        List<ExportRecurrenceRuleResponse> recurrenceRules,
        List<ExportCompletionResponse> completionHistory
) {
}
