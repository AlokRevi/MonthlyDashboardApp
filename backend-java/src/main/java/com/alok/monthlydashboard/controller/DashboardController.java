package com.alok.monthlydashboard.controller;

import com.alok.monthlydashboard.common.enums.ScaleNumbering;
import com.alok.monthlydashboard.common.enums.StartOfWeek;
import com.alok.monthlydashboard.common.enums.TimelineView;
import com.alok.monthlydashboard.dto.dashboard.MonthlyDashboardResponse;
import com.alok.monthlydashboard.dto.dashboard.TimelineDashboardResponse;
import com.alok.monthlydashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/monthly")
    public MonthlyDashboardResponse getMonthlyDashboard(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return dashboardService.getMonthlyDashboard(year, month);
    }

    @GetMapping("/timeline")
    public TimelineDashboardResponse getTimelineDashboard(
            @RequestParam(defaultValue = "MONTH") TimelineView view,
            @RequestParam(defaultValue = "SUNDAY") StartOfWeek startOfWeek,
            @RequestParam(defaultValue = "SEGMENT") ScaleNumbering scaleNumbering,
            @RequestParam(defaultValue = "true") boolean calendarYearBound
    ) {
        return dashboardService.getTimelineDashboard(
                view,
                startOfWeek,
                scaleNumbering,
                calendarYearBound
        );
    }
}
