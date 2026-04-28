package com.alok.monthlydashboard.service;

import com.alok.monthlydashboard.dto.export.SystemExportResponse;

public interface ExportService {

    SystemExportResponse exportSystemState();
}
