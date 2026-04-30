package com.alok.monthlydashboard.dto.category;

import com.alok.monthlydashboard.common.enums.CategoryRequires;
import com.alok.monthlydashboard.common.enums.FeelsLikeLabel;

import java.time.LocalDateTime;
import java.util.List;

public record CategoryResponse(
        Long id,
        String name,
        String color,
        CategoryRequires requires,
        List<FeelsLikeLabel> feelsLike,
        int taskCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
