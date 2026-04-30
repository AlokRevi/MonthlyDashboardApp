package com.alok.monthlydashboard.util;

import com.alok.monthlydashboard.entity.Task;
import com.alok.monthlydashboard.entity.TaskFixedDate;
import com.alok.monthlydashboard.entity.TaskRecurrenceRule;
import com.alok.monthlydashboard.entity.enums.IntervalUnit;
import com.alok.monthlydashboard.entity.enums.WeekOfMonth;
import com.alok.monthlydashboard.entity.enums.Weekday;

import java.util.List;
import java.util.Locale;

public final class RecurrenceSummaryHelper {

    private RecurrenceSummaryHelper() {
    }

    public static String summarize(Task task) {
        return switch (task.getRecurrenceType()) {
            case FIXED_DATE -> summarizeFixedDate(task);
            case INTERVAL -> summarizeInterval(task.getRecurrenceRule());
            case WEEKDAY -> summarizeWeekday(task.getRecurrenceRule());
        };
    }

    private static String summarizeFixedDate(Task task) {
        List<Integer> days = task.getFixedDates()
                .stream()
                .map(TaskFixedDate::getDayOfMonth)
                .sorted()
                .toList();

        if (days.isEmpty()) {
            return "fixed dates";
        }

        return "every " + joinNatural(days.stream()
                .map(RecurrenceSummaryHelper::ordinal)
                .toList());
    }

    private static String summarizeInterval(TaskRecurrenceRule rule) {
        if (rule == null || rule.getIntervalValue() == null || rule.getIntervalUnit() == null) {
            return "custom interval";
        }

        int value = rule.getIntervalValue();
        String unit = intervalUnitLabel(rule.getIntervalUnit(), value);

        if (value == 1) {
            return "every " + unit;
        }

        return "every " + value + " " + unit;
    }

    private static String summarizeWeekday(TaskRecurrenceRule rule) {
        if (rule == null || rule.getWeekday() == null || rule.getWeekOfMonth() == null) {
            return "weekday pattern";
        }

        return weekOfMonthLabel(rule.getWeekOfMonth())
                + " "
                + weekdayLabel(rule.getWeekday());
    }

    private static String intervalUnitLabel(IntervalUnit unit, int value) {
        return switch (unit) {
            case DAYS -> value == 1 ? "day" : "days";
            case WEEKS -> value == 1 ? "week" : "weeks";
            case MONTHS -> value == 1 ? "month" : "months";
        };
    }

    private static String weekOfMonthLabel(WeekOfMonth weekOfMonth) {
        return switch (weekOfMonth) {
            case FIRST -> "first";
            case SECOND -> "second";
            case THIRD -> "third";
            case FOURTH -> "fourth";
            case LAST -> "last";
        };
    }

    private static String weekdayLabel(Weekday weekday) {
        return weekday.name().toLowerCase(Locale.ENGLISH);
    }

    private static String ordinal(int day) {
        if (day % 100 >= 11 && day % 100 <= 13) {
            return day + "th";
        }

        return switch (day % 10) {
            case 1 -> day + "st";
            case 2 -> day + "nd";
            case 3 -> day + "rd";
            default -> day + "th";
        };
    }

    private static String joinNatural(List<String> values) {
        if (values.size() == 1) {
            return values.get(0);
        }

        if (values.size() == 2) {
            return values.get(0) + " and " + values.get(1);
        }

        return String.join(", ", values.subList(0, values.size() - 1))
                + ", and "
                + values.get(values.size() - 1);
    }
}
