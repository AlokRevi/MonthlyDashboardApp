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
    void februaryTwentyNinthOnlyOccursInLeapYearsWithoutFallback() {
        Task task = fixedDateTask(103L, LocalDate.of(2023, 1, 1), false, 29);
        when(taskRepository.findById(103L)).thenReturn(Optional.of(task));

        assertThat(recurrenceService.generateOccurrenceDatesForMonth(103L, 2024, 2))
                .containsExactly(LocalDate.of(2024, 2, 29));
        assertThat(recurrenceService.generateOccurrenceDatesForMonth(103L, 2025, 2))
                .isEmpty();
    }

    @Test
    void februaryTwentyNinthFallsBackToLastDayInNonLeapYearsWhenEnabled() {
        Task task = fixedDateTask(104L, LocalDate.of(2023, 1, 1), true, 29);
        when(taskRepository.findById(104L)).thenReturn(Optional.of(task));

        assertThat(recurrenceService.generateOccurrenceDatesForMonth(104L, 2025, 2))
                .containsExactly(LocalDate.of(2025, 2, 28));
    }

    @Test
    void fixedDatesOnThirtiethAndThirtyFirstFallbackToOneLastDayInShorterMonth() {
        Task task = fixedDateTask(105L, LocalDate.of(2026, 1, 1), true, 30, 31);
        when(taskRepository.findById(105L)).thenReturn(Optional.of(task));

        assertThat(recurrenceService.generateOccurrenceDatesForMonth(105L, 2026, 2))
                .containsExactly(LocalDate.of(2026, 2, 28));
        assertThat(recurrenceService.generateOccurrenceDatesForMonth(105L, 2026, 4))
                .containsExactly(LocalDate.of(2026, 4, 30));
    }

    @Test
    void fixedDateDoesNotFallbackWhenFallbackIsDisabled() {
        Task task = fixedDateTask(106L, LocalDate.of(2026, 1, 1), false, 31);
        when(taskRepository.findById(106L)).thenReturn(Optional.of(task));

        assertThat(recurrenceService.generateOccurrenceDatesForMonth(106L, 2026, 2))
                .isEmpty();
        assertThat(recurrenceService.generateOccurrenceDatesForMonth(106L, 2026, 4))
                .isEmpty();
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
    void intervalTaskContinuesCadenceAcrossYearBoundary() {
        Task task = intervalTask(107L, LocalDate.of(2025, 12, 29), 3);
        when(taskRepository.findById(107L)).thenReturn(Optional.of(task));

        List<LocalDate> dates = recurrenceService.generateOccurrenceDatesForMonth(107L, 2026, 1);

        assertThat(dates).containsExactly(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 4),
                LocalDate.of(2026, 1, 7),
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 13),
                LocalDate.of(2026, 1, 16),
                LocalDate.of(2026, 1, 19),
                LocalDate.of(2026, 1, 22),
                LocalDate.of(2026, 1, 25),
                LocalDate.of(2026, 1, 28),
                LocalDate.of(2026, 1, 31)
        );
    }

    @Test
    void monthlyIntervalFallsBackToLastValidDayInShortMonthsWithoutLosingAnchorDay() {
        Task task = intervalTask(
                113L,
                LocalDate.of(2026, 1, 31),
                1,
                com.alok.monthlydashboard.entity.enums.IntervalUnit.MONTHS
        );
        when(taskRepository.findById(113L)).thenReturn(Optional.of(task));

        assertThat(recurrenceService.generateOccurrenceDatesForMonth(113L, 2026, 2))
                .containsExactly(LocalDate.of(2026, 2, 28));
        assertThat(recurrenceService.generateOccurrenceDatesForMonth(113L, 2026, 3))
                .containsExactly(LocalDate.of(2026, 3, 31));
        assertThat(recurrenceService.generateOccurrenceDatesForMonth(113L, 2026, 4))
                .containsExactly(LocalDate.of(2026, 4, 30));
    }

    @Test
    void monthlyIntervalUsesLeapDayFallbackWhenFebruaryHasTwentyNineDays() {
        Task task = intervalTask(
                114L,
                LocalDate.of(2024, 1, 31),
                1,
                com.alok.monthlydashboard.entity.enums.IntervalUnit.MONTHS
        );
        when(taskRepository.findById(114L)).thenReturn(Optional.of(task));

        assertThat(recurrenceService.generateOccurrenceDatesForMonth(114L, 2024, 2))
                .containsExactly(LocalDate.of(2024, 2, 29));
    }

    @Test
    void monthlyIntervalKeepsCadenceAcrossYearBoundary() {
        Task task = intervalTask(
                115L,
                LocalDate.of(2025, 11, 30),
                1,
                com.alok.monthlydashboard.entity.enums.IntervalUnit.MONTHS
        );
        when(taskRepository.findById(115L)).thenReturn(Optional.of(task));

        assertThat(recurrenceService.generateOccurrenceDatesForMonth(115L, 2025, 12))
                .containsExactly(LocalDate.of(2025, 12, 30));
        assertThat(recurrenceService.generateOccurrenceDatesForMonth(115L, 2026, 1))
                .containsExactly(LocalDate.of(2026, 1, 30));
    }

    @Test
    void monthlyIntervalRespectsIntervalValueAcrossMonths() {
        Task task = intervalTask(
                116L,
                LocalDate.of(2026, 1, 31),
                2,
                com.alok.monthlydashboard.entity.enums.IntervalUnit.MONTHS
        );
        when(taskRepository.findById(116L)).thenReturn(Optional.of(task));

        assertThat(recurrenceService.generateOccurrenceDatesForMonth(116L, 2026, 2))
                .isEmpty();
        assertThat(recurrenceService.generateOccurrenceDatesForMonth(116L, 2026, 3))
                .containsExactly(LocalDate.of(2026, 3, 31));
        assertThat(recurrenceService.generateOccurrenceDatesForMonth(116L, 2026, 5))
                .containsExactly(LocalDate.of(2026, 5, 31));
    }

    @Test
    void monthlyIntervalRespectsEndDateCutoff() {
        Task task = intervalTask(
                117L,
                LocalDate.of(2026, 1, 31),
                1,
                com.alok.monthlydashboard.entity.enums.IntervalUnit.MONTHS
        );
        task.setEndDate(LocalDate.of(2026, 3, 15));
        when(taskRepository.findById(117L)).thenReturn(Optional.of(task));

        assertThat(recurrenceService.generateOccurrenceDatesForMonth(117L, 2026, 2))
                .containsExactly(LocalDate.of(2026, 2, 28));
        assertThat(recurrenceService.generateOccurrenceDatesForMonth(117L, 2026, 3))
                .isEmpty();
    }

    @Test
    void weekdayRulesSelectRequestedOrdinalWeekday() {
        Task firstMonday = weekdayTask(
                108L,
                LocalDate.of(2026, 1, 1),
                com.alok.monthlydashboard.entity.enums.Weekday.MONDAY,
                com.alok.monthlydashboard.entity.enums.WeekOfMonth.FIRST
        );
        Task secondMonday = weekdayTask(
                109L,
                LocalDate.of(2026, 1, 1),
                com.alok.monthlydashboard.entity.enums.Weekday.MONDAY,
                com.alok.monthlydashboard.entity.enums.WeekOfMonth.SECOND
        );
        Task fourthMonday = weekdayTask(
                110L,
                LocalDate.of(2026, 1, 1),
                com.alok.monthlydashboard.entity.enums.Weekday.MONDAY,
                com.alok.monthlydashboard.entity.enums.WeekOfMonth.FOURTH
        );
        when(taskRepository.findById(108L)).thenReturn(Optional.of(firstMonday));
        when(taskRepository.findById(109L)).thenReturn(Optional.of(secondMonday));
        when(taskRepository.findById(110L)).thenReturn(Optional.of(fourthMonday));

        assertThat(recurrenceService.generateOccurrenceDatesForMonth(108L, 2026, 3))
                .containsExactly(LocalDate.of(2026, 3, 2));
        assertThat(recurrenceService.generateOccurrenceDatesForMonth(109L, 2026, 3))
                .containsExactly(LocalDate.of(2026, 3, 9));
        assertThat(recurrenceService.generateOccurrenceDatesForMonth(110L, 2026, 3))
                .containsExactly(LocalDate.of(2026, 3, 23));
    }

    @Test
    void lastWeekdayUsesFinalMatchingDayWhenFifthExists() {
        Task task = weekdayTask(
                111L,
                LocalDate.of(2026, 1, 1),
                com.alok.monthlydashboard.entity.enums.Weekday.MONDAY,
                com.alok.monthlydashboard.entity.enums.WeekOfMonth.LAST
        );
        when(taskRepository.findById(111L)).thenReturn(Optional.of(task));

        assertThat(recurrenceService.generateOccurrenceDatesForMonth(111L, 2026, 3))
                .containsExactly(LocalDate.of(2026, 3, 30));
    }

    @Test
    void recurrenceDoesNotGenerateBeforeStartDateOrAfterEndDate() {
        Task task = fixedDateTask(112L, LocalDate.of(2026, 4, 15), true, 10, 20);
        task.setEndDate(LocalDate.of(2026, 5, 15));
        when(taskRepository.findById(112L)).thenReturn(Optional.of(task));

        assertThat(recurrenceService.generateOccurrenceDatesForMonth(112L, 2026, 3))
                .isEmpty();
        assertThat(recurrenceService.generateOccurrenceDatesForMonth(112L, 2026, 4))
                .containsExactly(LocalDate.of(2026, 4, 20));
        assertThat(recurrenceService.generateOccurrenceDatesForMonth(112L, 2026, 5))
                .containsExactly(LocalDate.of(2026, 5, 10));
        assertThat(recurrenceService.generateOccurrenceDatesForMonth(112L, 2026, 6))
                .isEmpty();
    }

    @Test
    void fixedDateRecurrenceWorksAcrossCustomDateRange() {
        Task task = fixedDateTask(118L, LocalDate.of(2026, 1, 1), true, 10, 20);
        when(taskRepository.findById(118L)).thenReturn(Optional.of(task));

        assertThat(recurrenceService.generateOccurrenceDatesBetween(
                118L,
                LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 4, 15)
        )).containsExactly(
                LocalDate.of(2026, 3, 20),
                LocalDate.of(2026, 4, 10)
        );
    }

    @Test
    void intervalRecurrenceWorksAcrossCustomDateRange() {
        Task task = intervalTask(119L, LocalDate.of(2025, 12, 29), 3);
        when(taskRepository.findById(119L)).thenReturn(Optional.of(task));

        assertThat(recurrenceService.generateOccurrenceDatesBetween(
                119L,
                LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 1, 12)
        )).containsExactly(
                LocalDate.of(2026, 1, 7),
                LocalDate.of(2026, 1, 10)
        );
    }

    @Test
    void weekdayRecurrenceWorksAcrossCustomDateRange() {
        Task task = weekdayTask(
                120L,
                LocalDate.of(2026, 1, 1),
                com.alok.monthlydashboard.entity.enums.Weekday.MONDAY,
                com.alok.monthlydashboard.entity.enums.WeekOfMonth.FIRST
        );
        when(taskRepository.findById(120L)).thenReturn(Optional.of(task));

        assertThat(recurrenceService.generateOccurrenceDatesBetween(
                120L,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 30)
        )).containsExactly(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 4, 6)
        );
    }

    @Test
    void customDateRangeCanCrossMonthBoundaryAndPreserveFallbackRules() {
        Task task = fixedDateTask(121L, LocalDate.of(2026, 1, 1), true, 31);
        when(taskRepository.findById(121L)).thenReturn(Optional.of(task));

        assertThat(recurrenceService.generateOccurrenceDatesBetween(
                121L,
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 2, 28)
        )).containsExactly(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28)
        );
    }

    @Test
    void customDateRangeCanCrossYearBoundary() {
        Task task = fixedDateTask(122L, LocalDate.of(2025, 1, 1), true, 31);
        when(taskRepository.findById(122L)).thenReturn(Optional.of(task));

        assertThat(recurrenceService.generateOccurrenceDatesBetween(
                122L,
                LocalDate.of(2025, 12, 15),
                LocalDate.of(2026, 1, 31)
        )).containsExactly(
                LocalDate.of(2025, 12, 31),
                LocalDate.of(2026, 1, 31)
        );
    }

    @Test
    void monthWrapperOutputMatchesEquivalentFullMonthDateRange() {
        Task task = fixedDateTask(123L, LocalDate.of(2026, 1, 1), true, 10, 20, 31);
        when(taskRepository.findById(123L)).thenReturn(Optional.of(task));

        assertThat(recurrenceService.generateOccurrenceDatesForMonth(123L, 2026, 4))
                .containsExactlyElementsOf(recurrenceService.generateOccurrenceDatesBetween(
                        123L,
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 30)
                ))
                .containsExactly(
                        LocalDate.of(2026, 4, 10),
                        LocalDate.of(2026, 4, 20),
                        LocalDate.of(2026, 4, 30)
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

    private Task fixedDateTask(Long id, LocalDate startDate, boolean fallbackToLastDay, int... daysOfMonth) {
        Task task = baseTask(id, RecurrenceType.FIXED_DATE, startDate);

        TaskRecurrenceRule rule = new TaskRecurrenceRule();
        rule.setFallbackToLastDay(fallbackToLastDay);
        task.setRecurrenceRule(rule);

        for (int dayOfMonth : daysOfMonth) {
            TaskFixedDate fixedDate = new TaskFixedDate();
            fixedDate.setDayOfMonth(dayOfMonth);
            task.addFixedDate(fixedDate);
        }

        return task;
    }

    private Task intervalTask(Long id, LocalDate startDate, int intervalDays) {
        return intervalTask(
                id,
                startDate,
                intervalDays,
                com.alok.monthlydashboard.entity.enums.IntervalUnit.DAYS
        );
    }

    private Task intervalTask(
            Long id,
            LocalDate startDate,
            int intervalValue,
            com.alok.monthlydashboard.entity.enums.IntervalUnit intervalUnit
    ) {
        Task task = baseTask(id, RecurrenceType.INTERVAL, startDate);

        TaskRecurrenceRule rule = new TaskRecurrenceRule();
        rule.setIntervalValue(intervalValue);
        rule.setIntervalUnit(intervalUnit);
        task.setRecurrenceRule(rule);

        return task;
    }

    private Task weekdayTask(
            Long id,
            LocalDate startDate,
            com.alok.monthlydashboard.entity.enums.Weekday weekday,
            com.alok.monthlydashboard.entity.enums.WeekOfMonth weekOfMonth
    ) {
        Task task = baseTask(id, RecurrenceType.WEEKDAY, startDate);

        TaskRecurrenceRule rule = new TaskRecurrenceRule();
        rule.setWeekday(weekday);
        rule.setWeekOfMonth(weekOfMonth);
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
