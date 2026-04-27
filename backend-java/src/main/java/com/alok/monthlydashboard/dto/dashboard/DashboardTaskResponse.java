package com.alok.monthlydashboard.dto.dashboard;
import com.alok.monthlydashboard.common.enums.RecurrenceType;
import java.util.List;

public record DashboardTaskResponse(
        Long taskId,
        String taskName,
        RecurrenceType recurrenceType,
        List<OccurrenceResponse> occurrences
) {
}