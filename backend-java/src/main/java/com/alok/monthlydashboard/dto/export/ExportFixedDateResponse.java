package com.alok.monthlydashboard.dto.export;

import java.time.LocalDateTime;

public record ExportFixedDateResponse(
        Long id,
        Integer dayOfMonth,
        LocalDateTime createdAt
) {
}
