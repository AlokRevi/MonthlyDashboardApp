package com.alok.monthlydashboard.util;

import com.alok.monthlydashboard.dto.task.*;
import com.alok.monthlydashboard.entity.Task;
import com.alok.monthlydashboard.entity.TaskFixedDate;
import com.alok.monthlydashboard.entity.TaskRecurrenceRule;
import com.alok.monthlydashboard.common.enums.*;

import java.util.List;

public final class TaskMapper {

    private TaskMapper() {
    }

    public static TaskSummaryResponse toTaskSummaryResponse(Task task) {
        return new TaskSummaryResponse(
                task.getId(),
                task.getCategory().getId(),
                task.getCategory().getName(),
                task.getName(),
                RecurrenceType.valueOf(task.getRecurrenceType().name()),
                task.getStartDate(),
                task.getEndDate(),
                task.isActive()
        );
    }

    public static TaskDetailResponse toTaskDetailResponse(Task task) {
        return new TaskDetailResponse(
                task.getId(),
                task.getCategory().getId(),
                task.getCategory().getName(),
                task.getName(),
                task.getDescription(),
                RecurrenceType.valueOf(task.getRecurrenceType().name()),
                task.getStartDate(),
                task.getEndDate(),
                task.isActive(),
                toTaskRuleRequest(task)
        );
    }

    public static TaskRuleRequest toTaskRuleRequest(Task task) {
        TaskRecurrenceRule rule = task.getRecurrenceRule();

        List<Integer> fixedDates = task.getFixedDates() == null
                ? List.of()
                : task.getFixedDates().stream()
                .map(TaskFixedDate::getDayOfMonth)
                .sorted()
                .toList();

        if (rule == null) {
            return new TaskRuleRequest(
                    fixedDates,
                    null,
                    null,
                    null,
                    null,
                    true
            );
        }

        return new TaskRuleRequest(
                fixedDates,
                rule.getIntervalValue(),
                rule.getIntervalUnit() == null ? null : com.alok.monthlydashboard.common.enums.IntervalUnit.valueOf(rule.getIntervalUnit().name()),
                rule.getWeekday() == null ? null : Weekday.valueOf(rule.getWeekday().name()),
                rule.getWeekOfMonth() == null ? null : WeekOfMonth.valueOf(rule.getWeekOfMonth().name()),
                rule.isFallbackToLastDay()
        );
    }
}