package com.alok.monthlydashboard.util;

import com.alok.monthlydashboard.dto.task.TaskRuleRequest;
import com.alok.monthlydashboard.common.enums.RecurrenceType;
import com.alok.monthlydashboard.exception.ValidationException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TaskValidationHelper {

    private TaskValidationHelper() {
    }

    public static void validateRule(RecurrenceType recurrenceType, TaskRuleRequest rule) {
        if (recurrenceType == null) {
            throw new ValidationException("recurrenceType is required");
        }

        if (rule == null) {
            throw new ValidationException("rule is required");
        }

        switch (recurrenceType) {
            case FIXED_DATE -> validateFixedDateRule(rule);
            case INTERVAL -> validateIntervalRule(rule);
            case WEEKDAY -> validateWeekdayRule(rule);
        }
    }

    private static void validateFixedDateRule(TaskRuleRequest rule) {
        List<Integer> fixedDates = rule.fixedDates();

        if (fixedDates == null || fixedDates.isEmpty()) {
            throw new ValidationException("fixedDates must contain at least one day for FIXED_DATE tasks");
        }

        Set<Integer> uniqueDays = new HashSet<>();
        for (Integer day : fixedDates) {
            if (day == null || day < 1 || day > 31) {
                throw new ValidationException("Each fixed date must be between 1 and 31");
            }
            if (!uniqueDays.add(day)) {
                throw new ValidationException("Duplicate values are not allowed in fixedDates");
            }
        }
    }

    private static void validateIntervalRule(TaskRuleRequest rule) {
        if (rule.intervalValue() == null || rule.intervalValue() <= 0) {
            throw new ValidationException("intervalValue must be greater than 0 for INTERVAL tasks");
        }

        if (rule.intervalUnit() == null) {
            throw new ValidationException("intervalUnit is required for INTERVAL tasks");
        }
    }

    private static void validateWeekdayRule(TaskRuleRequest rule) {
        if (rule.weekday() == null) {
            throw new ValidationException("weekday is required for WEEKDAY tasks");
        }

        if (rule.weekOfMonth() == null) {
            throw new ValidationException("weekOfMonth is required for WEEKDAY tasks");
        }
    }
}