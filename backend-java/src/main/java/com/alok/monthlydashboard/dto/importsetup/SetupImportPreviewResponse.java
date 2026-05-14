package com.alok.monthlydashboard.dto.importsetup;

import java.util.List;

public record SetupImportPreviewResponse(
        boolean valid,
        String version,
        int categoryCount,
        int taskCount,
        int activeTaskCount,
        int inactiveTaskCount,
        List<String> warnings,
        List<String> errors
) {
}
