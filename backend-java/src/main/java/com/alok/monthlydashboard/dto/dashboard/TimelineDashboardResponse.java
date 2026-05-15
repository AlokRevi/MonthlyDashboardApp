package com.alok.monthlydashboard.dto.dashboard;

import com.alok.monthlydashboard.common.enums.TimelineView;

import java.time.LocalDate;
import java.util.List;

public record TimelineDashboardResponse(
        TimelineView view,
        int year,
        int month,
        LocalDate startDate,
        LocalDate endDate,
        String label,
        LocalDate today,
        boolean readOnly,
        TimelineSettingsResponse settings,
        TimelineScaleBarResponse scaleBar,
        List<TimelineCellResponse> cells,
        List<TimelineCategoryResponse> categories
) {
}
