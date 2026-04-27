package com.alok.monthlydashboard.controller;

import com.alok.monthlydashboard.dto.dashboard.MonthlyDashboardResponse;
import com.alok.monthlydashboard.dto.history.TaskHistoryResponse;
import com.alok.monthlydashboard.service.HistoryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/history/monthly")
    public MonthlyDashboardResponse getPastMonth(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return historyService.getPastMonth(year, month);
    }

    @GetMapping("/tasks/{taskId}/history")
    public TaskHistoryResponse getTaskHistory(@PathVariable Long taskId) {
        return historyService.getTaskHistory(taskId);
    }
}