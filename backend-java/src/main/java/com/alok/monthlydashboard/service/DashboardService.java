package com.alok.monthlydashboard.service;

import com.alok.monthlydashboard.common.enums.ScaleNumbering;
import com.alok.monthlydashboard.common.enums.StartOfWeek;
import com.alok.monthlydashboard.common.enums.TimelineView;
import com.alok.monthlydashboard.dto.dashboard.MonthlyDashboardResponse;
import com.alok.monthlydashboard.dto.dashboard.TimelineDashboardResponse;

public interface DashboardService {
    MonthlyDashboardResponse getMonthlyDashboard(int year, int month);

    TimelineDashboardResponse getTimelineDashboard(
            TimelineView view,
            StartOfWeek startOfWeek,
            ScaleNumbering scaleNumbering,
            boolean calendarYearBound
    );
}
