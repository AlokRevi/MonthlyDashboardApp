package com.alok.monthlydashboard.controller;

import com.alok.monthlydashboard.dto.export.SystemExportResponse;
import com.alok.monthlydashboard.service.ExportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/export")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping
    public SystemExportResponse exportSystemState() {
        return exportService.exportSystemState();
    }
}
