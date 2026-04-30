package com.alok.monthlydashboard.dto.export;

import com.alok.monthlydashboard.common.enums.CategoryRequires;
import com.alok.monthlydashboard.common.enums.FeelsLikeLabel;

import java.time.LocalDateTime;
import java.util.List;

public record ExportCategoryResponse(
        Long id,
        String name,
        String color,
        CategoryRequires requires,
        List<FeelsLikeLabel> feelsLike,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
