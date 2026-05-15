package com.alok.monthlydashboard.dto.dashboard;

import com.alok.monthlydashboard.common.enums.ScaleNumbering;
import com.alok.monthlydashboard.common.enums.StartOfWeek;
import com.alok.monthlydashboard.common.enums.TimelineView;

public record TimelineSettingsResponse(
        TimelineView view,
        StartOfWeek startOfWeek,
        ScaleNumbering scaleNumbering,
        boolean calendarYearBound
) {
}
