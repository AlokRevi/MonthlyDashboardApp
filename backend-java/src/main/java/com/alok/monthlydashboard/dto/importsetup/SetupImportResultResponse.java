package com.alok.monthlydashboard.dto.importsetup;

import java.util.List;

public record SetupImportResultResponse(
        boolean imported,
        String mode,
        int categoryCount,
        int taskCount,
        int activeTaskCount,
        int inactiveTaskCount,
        List<String> warnings,
        List<String> errors
) {
}
