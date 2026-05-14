package com.alok.monthlydashboard.controller;

import com.alok.monthlydashboard.common.enums.SetupImportMode;
import com.alok.monthlydashboard.dto.importsetup.SetupImportPreviewResponse;
import com.alok.monthlydashboard.dto.importsetup.SetupImportRequest;
import com.alok.monthlydashboard.dto.importsetup.SetupImportResultResponse;
import com.alok.monthlydashboard.service.SetupImportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/import/setup")
public class SetupImportController {

    private final SetupImportService setupImportService;

    public SetupImportController(SetupImportService setupImportService) {
        this.setupImportService = setupImportService;
    }

    @PostMapping("/preview")
    public SetupImportPreviewResponse previewSetupImport(@RequestBody SetupImportRequest request) {
        return setupImportService.previewSetupImport(request);
    }

    @PostMapping
    public SetupImportResultResponse importSetup(
            @RequestBody SetupImportRequest request,
            @RequestParam(defaultValue = "EMPTY_ONLY") SetupImportMode mode
    ) {
        return setupImportService.importSetup(request, mode);
    }
}
