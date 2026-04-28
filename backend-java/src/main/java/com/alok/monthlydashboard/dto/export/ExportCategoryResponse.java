package com.alok.monthlydashboard.dto.export;

import java.time.LocalDateTime;

public record ExportCategoryResponse(
        Long id,
        String name,
        String color,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
