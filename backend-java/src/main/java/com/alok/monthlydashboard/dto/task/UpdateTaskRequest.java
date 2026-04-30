package com.alok.monthlydashboard.dto.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.alok.monthlydashboard.common.enums.RecurrenceType;
import com.alok.monthlydashboard.common.enums.TaskEditScope;
import java.time.LocalDate;

public record UpdateTaskRequest(
        @NotNull
        Long categoryId,

        @NotBlank
        @Size(max = 150)
        String name,

        String description,

        @NotNull
        RecurrenceType recurrenceType,

        @NotNull
        LocalDate startDate,

        LocalDate endDate,

        Boolean isActive,

        TaskEditScope editScope,

        LocalDate selectedOccurrenceDate,

        @Valid
        @NotNull
        TaskRuleRequest rule
) {
}
