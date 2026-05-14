package com.alok.monthlydashboard.service;

import com.alok.monthlydashboard.common.enums.SetupImportMode;
import com.alok.monthlydashboard.dto.importsetup.SetupImportPreviewResponse;
import com.alok.monthlydashboard.dto.importsetup.SetupImportRequest;
import com.alok.monthlydashboard.dto.importsetup.SetupImportResultResponse;

public interface SetupImportService {

    SetupImportPreviewResponse previewSetupImport(SetupImportRequest request);

    SetupImportResultResponse importSetup(SetupImportRequest request, SetupImportMode mode);
}
