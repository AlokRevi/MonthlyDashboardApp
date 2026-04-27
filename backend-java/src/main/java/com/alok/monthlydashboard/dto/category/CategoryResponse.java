package com.alok.monthlydashboard.dto.category;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        String color,
        int taskCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}