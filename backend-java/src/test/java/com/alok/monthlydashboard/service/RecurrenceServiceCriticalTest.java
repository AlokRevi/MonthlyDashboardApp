package com.alok.monthlydashboard.service;

import com.alok.monthlydashboard.common.enums.OccurrenceStatus;
import com.alok.monthlydashboard.common.enums.RecurrenceType;
import com.alok.monthlydashboard.dto.dashboard.OccurrenceResponse;
import com.alok.monthlydashboard.entity.Task;
import com.alok.monthlydashboard.entity.TaskFixedDate;
import com.alok.monthlydashboard.entity.TaskRecurrenceRule;
import com.alok.monthlydashboard.repository.TaskCompletionRepository;
import com.alok.monthlydashboard.repository.TaskRepository;
import com.alok.monthlydashboard.service.impl.RecurrenceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class RecurrenceServiceCriticalTest {

    private TaskRepository taskRepository;
    private TaskCompletionRepository taskCompletionRepository;
    private RecurrenceServiceImpl recurrenceService;

    @BeforeEach
    void setUp() {
        taskRepository = Mockito.mock(TaskRepository.class);
        taskCompletionRepository = Mockito.mock(TaskCompletionRepository.class);
        recurrenceService = new RecurrenceServiceImpl(
                taskRepository,
                taskCompletionRepository,
                () -> LocalDate.of(2026, 4, 28)
        );

        when(taskCompletionRepository.findByTaskIdAndOccurrenceDate(anyLong(), any(LocalDate.class)))
                .thenReturn(Optional.empty());
    }

    @Test
    void fixedDateWithFallbackUsesLastDayWhenMonthDoesNotHaveRequestedDay() {
        Task task = fixedDateTask(100L, LocalDate.of(2026, 1, 1), true, 31);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

        List<LocalDate> dates = recurrenceService.generateOccurrenceDatesForMonth(100L, 2026, 2);

        assertThat(dates).containsExactly(LocalDate.of(2026, 2, 28));
    }

    @Test
    void intervalTaskContinuesCadenceAcrossMonthBoundary() {
        Task task = intervalTask(101L, LocalDate.of(2026, 3, 30), 7);
        when(taskRepository.findById(101L)).thenReturn(Optional.of(task));

        List<LocalDate> dates = recurrenceService.generateOccurrenceDatesForMonth(101L, 2026, 4);

        assertThat(dates).containsExactly(
                LocalDate.of(2026, 4, 6),
                LocalDate.of(2026, 4, 13),
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 27)
        );
    }

    @Test
    void pastIncompleteOccurrenceIsMarkedOverdue() {
        LocalDate yesterday = LocalDate.of(2026, 4, 27);
        Task task = fixedDateTask(102L, yesterday, true, yesterday.getDayOfMonth());
        when(taskRepository.findById(102L)).thenReturn(Optional.of(task));

        YearMonth targetMonth = YearMonth.from(yesterday);
        List<OccurrenceResponse> occurrences = recurrenceService.generateOccurrencesForMonth(
                102L,
                targetMonth.getYear(),
                targetMonth.getMonthValue()
        );

        assertThat(occurrences)
                .filteredOn(occurrence -> occurrence.occurrenceDate().equals(yesterday))
                .singleElement()
                .extracting(OccurrenceResponse::status)
                .isEqualTo(OccurrenceStatus.OVERDUE);
    }

    private Task fixedDateTask(Long id, LocalDate startDate, boolean fallbackToLastDay, int dayOfMonth) {
        Task task = baseTask(id, RecurrenceType.FIXED_DATE, startDate);

        TaskRecurrenceRule rule = new TaskRecurrenceRule();
        rule.setFallbackToLastDay(fallbackToLastDay);
        task.setRecurrenceRule(rule);

        TaskFixedDate fixedDate = new TaskFixedDate();
        fixedDate.setDayOfMonth(dayOfMonth);
        task.addFixedDate(fixedDate);

        return task;
    }

    private Task intervalTask(Long id, LocalDate startDate, int intervalDays) {
        Task task = baseTask(id, RecurrenceType.INTERVAL, startDate);

        TaskRecurrenceRule rule = new TaskRecurrenceRule();
        rule.setIntervalValue(intervalDays);
        rule.setIntervalUnit(com.alok.monthlydashboard.entity.enums.IntervalUnit.DAYS);
        task.setRecurrenceRule(rule);

        return task;
    }

    private Task baseTask(Long id, RecurrenceType recurrenceType, LocalDate startDate) {
        Task task = new Task();
        ReflectionTestUtils.setField(task, "id", id);
        task.setName("Recurring Task");
        task.setRecurrenceType(recurrenceType);
        task.setStartDate(startDate);
        task.setActive(true);
        return task;
    }
}
