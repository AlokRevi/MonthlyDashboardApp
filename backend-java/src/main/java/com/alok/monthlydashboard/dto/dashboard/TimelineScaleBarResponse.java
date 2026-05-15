package com.alok.monthlydashboard.dto.dashboard;

import java.util.List;

public record TimelineScaleBarResponse(
        List<String> anchorCellKeys,
        String currentDateLabel
) {
}
