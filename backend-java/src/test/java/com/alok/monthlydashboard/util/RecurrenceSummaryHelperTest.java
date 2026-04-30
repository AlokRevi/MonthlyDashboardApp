package com.alok.monthlydashboard.util;

import com.alok.monthlydashboard.common.enums.RecurrenceType;
import com.alok.monthlydashboard.entity.Task;
import com.alok.monthlydashboard.entity.TaskFixedDate;
import com.alok.monthlydashboard.entity.TaskRecurrenceRule;
import com.alok.monthlydashboard.entity.enums.IntervalUnit;
import com.alok.monthlydashboard.entity.enums.WeekOfMonth;
import com.alok.monthlydashboard.entity.enums.Weekday;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RecurrenceSummaryHelperTest {

    @Test
    void summarizesFixedDateRule() {
        Task task = fixedDateTask(15);

        assertThat(RecurrenceSummaryHelper.summarize(task)).isEqualTo("every 15th");
    }

    @Test
    void summarizesMultipleFixedDatesWithOrdinals() {
        Task task = fixedDateTask(1, 11, 22);

        assertThat(RecurrenceSummaryHelper.summarize(task)).isEqualTo("every 1st, 11th, and 22nd");
    }

    @Test
    void summarizesIntervalRule() {
        Task task = intervalTask(2, IntervalUnit.WEEKS);

        assertThat(RecurrenceSummaryHelper.summarize(task)).isEqualTo("every 2 weeks");
    }

    @Test
    void summarizesSingularIntervalRule() {
        Task task = intervalTask(1, IntervalUnit.DAYS);

        assertThat(RecurrenceSummaryHelper.summarize(task)).isEqualTo("every day");
    }

    @Test
    void summarizesMonthlyIntervalRule() {
        Task task = intervalTask(3, IntervalUnit.MONTHS);

        assertThat(RecurrenceSummaryHelper.summarize(task)).isEqualTo("every 3 months");
    }

    @Test
    void summarizesSingularMonthlyIntervalRule() {
        Task task = intervalTask(1, IntervalUnit.MONTHS);

        assertThat(RecurrenceSummaryHelper.summarize(task)).isEqualTo("every month");
    }

    @Test
    void summarizesWeekdayRule() {
        Task task = weekdayTask(Weekday.SATURDAY, WeekOfMonth.SECOND);

        assertThat(RecurrenceSummaryHelper.summarize(task)).isEqualTo("second saturday");
    }

    @Test
    void summarizesLastWeekdayRule() {
        Task task = weekdayTask(Weekday.WEDNESDAY, WeekOfMonth.LAST);

        assertThat(RecurrenceSummaryHelper.summarize(task)).isEqualTo("last wednesday");
    }

    private Task fixedDateTask(int... days) {
        Task task = baseTask(RecurrenceType.FIXED_DATE);

        TaskRecurrenceRule rule = new TaskRecurrenceRule();
        rule.setFallbackToLastDay(true);
        task.setRecurrenceRule(rule);

        for (int day : days) {
            TaskFixedDate fixedDate = new TaskFixedDate();
            fixedDate.setDayOfMonth(day);
            task.addFixedDate(fixedDate);
        }

        return task;
    }

    private Task intervalTask(int value, IntervalUnit unit) {
        Task task = baseTask(RecurrenceType.INTERVAL);

        TaskRecurrenceRule rule = new TaskRecurrenceRule();
        rule.setIntervalValue(value);
        rule.setIntervalUnit(unit);
        task.setRecurrenceRule(rule);

        return task;
    }

    private Task weekdayTask(Weekday weekday, WeekOfMonth weekOfMonth) {
        Task task = baseTask(RecurrenceType.WEEKDAY);

        TaskRecurrenceRule rule = new TaskRecurrenceRule();
        rule.setWeekday(weekday);
        rule.setWeekOfMonth(weekOfMonth);
        task.setRecurrenceRule(rule);

        return task;
    }

    private Task baseTask(RecurrenceType recurrenceType) {
        Task task = new Task();
        task.setName("Summary Task");
        task.setRecurrenceType(recurrenceType);
        task.setStartDate(LocalDate.of(2026, 1, 1));
        task.setActive(true);
        return task;
    }
}
