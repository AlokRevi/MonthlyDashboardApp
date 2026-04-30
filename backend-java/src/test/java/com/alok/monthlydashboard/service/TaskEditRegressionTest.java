package com.alok.monthlydashboard.service;

import com.alok.monthlydashboard.common.enums.IntervalUnit;
import com.alok.monthlydashboard.common.enums.RecurrenceType;
import com.alok.monthlydashboard.common.enums.TaskEditScope;
import com.alok.monthlydashboard.common.enums.WeekOfMonth;
import com.alok.monthlydashboard.common.enums.Weekday;
import com.alok.monthlydashboard.dto.dashboard.MonthlyDashboardResponse;
import com.alok.monthlydashboard.dto.dashboard.OccurrenceResponse;
import com.alok.monthlydashboard.dto.task.TaskRuleRequest;
import com.alok.monthlydashboard.dto.task.UpdateTaskRequest;
import com.alok.monthlydashboard.entity.Category;
import com.alok.monthlydashboard.entity.Task;
import com.alok.monthlydashboard.entity.TaskCompletion;
import com.alok.monthlydashboard.entity.TaskFixedDate;
import com.alok.monthlydashboard.entity.TaskRecurrenceRule;
import com.alok.monthlydashboard.repository.CategoryRepository;
import com.alok.monthlydashboard.repository.TaskCompletionRepository;
import com.alok.monthlydashboard.repository.TaskRepository;
import com.alok.monthlydashboard.service.impl.DashboardServiceImpl;
import com.alok.monthlydashboard.service.impl.RecurrenceServiceImpl;
import com.alok.monthlydashboard.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskEditRegressionTest {

    private TaskRepository taskRepository;
    private CategoryRepository categoryRepository;
    private TaskCompletionRepository taskCompletionRepository;
    private TaskServiceImpl taskService;
    private RecurrenceServiceImpl recurrenceService;

    private Category bills;
    private Category errands;
    private Task task;

    @BeforeEach
    void setUp() {
        taskRepository = Mockito.mock(TaskRepository.class);
        categoryRepository = Mockito.mock(CategoryRepository.class);
        taskCompletionRepository = Mockito.mock(TaskCompletionRepository.class);

        taskService = new TaskServiceImpl(taskRepository, categoryRepository, taskCompletionRepository);
        recurrenceService = new RecurrenceServiceImpl(
                taskRepository,
                taskCompletionRepository,
                () -> LocalDate.of(2026, 4, 28)
        );

        bills = category(1L, "Bills");
        errands = category(2L, "Errands");
        task = fixedDateTask(100L, bills, 15);

        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(bills));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(errands));
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(bills, errands));
        when(taskCompletionRepository.findByTaskIdAndOccurrenceDate(anyLong(), any(LocalDate.class)))
                .thenReturn(Optional.empty());
    }

    @Test
    void editingFixedDateMovesOccurrenceFromDay15ToDay20() {
        taskService.updateTask(100L, updateRequest(1L, RecurrenceType.FIXED_DATE,
                new TaskRuleRequest(List.of(20), null, null, null, null, true)));

        assertThat(daysInApril()).containsExactly(20);
    }

    @Test
    void editRequestWithoutActiveFlagKeepsTaskVisible() {
        taskService.updateTask(100L, updateRequest(1L, RecurrenceType.FIXED_DATE, null,
                new TaskRuleRequest(List.of(20), null, null, null, null, true)));

        assertThat(task.isActive()).isTrue();
        assertThat(daysInApril()).containsExactly(20);
    }

    @Test
    void editingIntervalFromSevenDaysToThreeDaysChangesOccurrences() {
        taskService.updateTask(100L, updateRequest(1L, RecurrenceType.INTERVAL,
                new TaskRuleRequest(null, 3, IntervalUnit.DAYS, null, null, null)));

        assertThat(daysInApril()).containsExactly(1, 4, 7, 10, 13, 16, 19, 22, 25, 28);
    }

    @Test
    void editingWeekdayFromLastFridayToFirstMondayMovesOccurrence() {
        taskService.updateTask(100L, updateRequest(1L, RecurrenceType.WEEKDAY,
                new TaskRuleRequest(null, null, null, Weekday.MONDAY, WeekOfMonth.FIRST, null)));

        assertThat(daysInApril()).containsExactly(6);
    }

    @Test
    void editingRecurrencePreservesPastCompletions() {
        TaskCompletion completion = new TaskCompletion();
        completion.setTask(task);
        completion.setOccurrenceDate(LocalDate.of(2026, 4, 15));
        completion.setCompletionDate(LocalDate.of(2026, 4, 16));
        task.addCompletion(completion);

        taskService.updateTask(100L, updateRequest(1L, RecurrenceType.FIXED_DATE,
                new TaskRuleRequest(List.of(20), null, null, null, null, true)));

        assertThat(task.getCompletions())
                .singleElement()
                .satisfies(savedCompletion -> {
                    assertThat(savedCompletion.getOccurrenceDate()).isEqualTo(LocalDate.of(2026, 4, 15));
                    assertThat(savedCompletion.getCompletionDate()).isEqualTo(LocalDate.of(2026, 4, 16));
                });
    }

    @Test
    void editingCategoryMovesTaskToNewDashboardCategory() {
        taskService.updateTask(100L, updateRequest(2L, RecurrenceType.FIXED_DATE,
                new TaskRuleRequest(List.of(20), null, null, null, null, true)));

        when(taskRepository.findByCategoryIdAndIsActiveOrderByNameAsc(anyLong(), Mockito.eq(true)))
                .thenAnswer(invocation -> {
                    Long categoryId = invocation.getArgument(0);
                    return task.getCategory().getId().equals(categoryId) ? List.of(task) : List.of();
                });

        DashboardService dashboardService = new DashboardServiceImpl(
                categoryRepository,
                taskRepository,
                recurrenceService,
                () -> LocalDate.of(2026, 4, 28)
        );

        MonthlyDashboardResponse dashboard = dashboardService.getMonthlyDashboard(2026, 4);

        assertThat(dashboard.categories())
                .filteredOn(category -> category.categoryId().equals(1L))
                .first()
                .extracting(category -> category.tasks().size())
                .isEqualTo(0);
        assertThat(dashboard.categories())
                .filteredOn(category -> category.categoryId().equals(2L))
                .first()
                .satisfies(category -> assertThat(category.tasks())
                        .singleElement()
                        .satisfies(dashboardTask -> {
                            assertThat(dashboardTask.taskId()).isEqualTo(100L);
                            assertThat(dashboardTask.occurrences())
                                    .extracting(OccurrenceResponse::dayOfMonth)
                                    .containsExactly(20);
                        }));
    }

    @Test
    void thisAndFollowingEditSplitsTaskAndPreservesPastCompletionOnOriginalTask() {
        TaskCompletion completion = new TaskCompletion();
        completion.setTask(task);
        completion.setOccurrenceDate(LocalDate.of(2026, 4, 15));
        completion.setCompletionDate(LocalDate.of(2026, 4, 15));
        task.addCompletion(completion);

        taskService.updateTask(100L, scopedUpdateRequest(
                TaskEditScope.THIS_AND_FOLLOWING,
                LocalDate.of(2026, 5, 15),
                2L,
                RecurrenceType.INTERVAL,
                new TaskRuleRequest(null, 2, IntervalUnit.WEEKS, null, null, null)
        ));

        assertThat(task.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 14));
        assertThat(task.getCompletions())
                .singleElement()
                .satisfies(savedCompletion -> {
                    assertThat(savedCompletion.getTask()).isSameAs(task);
                    assertThat(savedCompletion.getOccurrenceDate()).isEqualTo(LocalDate.of(2026, 4, 15));
                    assertThat(savedCompletion.getCompletionDate()).isEqualTo(LocalDate.of(2026, 4, 15));
                });

        verify(taskRepository, times(2)).save(any(Task.class));
    }

    @Test
    void thisAndFollowingEditCreatesSuccessorTaskStartingAtSelectedOccurrence() {
        taskService.updateTask(100L, scopedUpdateRequest(
                TaskEditScope.THIS_AND_FOLLOWING,
                LocalDate.of(2026, 5, 15),
                2L,
                RecurrenceType.INTERVAL,
                new TaskRuleRequest(null, 2, IntervalUnit.WEEKS, null, null, null)
        ));

        Mockito.verify(taskRepository, times(1)).save(Mockito.argThat(savedTask ->
                savedTask.getStartDate().equals(LocalDate.of(2026, 5, 15))
                        && savedTask.getEndDate() == null
                        && savedTask.getCategory().equals(errands)
                        && savedTask.getRecurrenceType() == RecurrenceType.INTERVAL
                        && savedTask.getRecurrenceRule().getIntervalValue().equals(2)
                        && savedTask.getRecurrenceRule().getIntervalUnit()
                        == com.alok.monthlydashboard.entity.enums.IntervalUnit.WEEKS
        ));
    }

    @Test
    void allFutureScopeUsesSameSafeSplitBehaviorWithoutRewritingPastTask() {
        taskService.updateTask(100L, scopedUpdateRequest(
                TaskEditScope.ALL_FUTURE,
                LocalDate.of(2026, 5, 15),
                1L,
                RecurrenceType.FIXED_DATE,
                new TaskRuleRequest(List.of(20), null, null, null, null, true)
        ));

        assertThat(task.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 14));
        assertThat(task.getFixedDates())
                .singleElement()
                .extracting(TaskFixedDate::getDayOfMonth)
                .isEqualTo(15);
    }

    private List<Integer> daysInApril() {
        return recurrenceService.generateOccurrencesForMonth(100L, 2026, 4)
                .stream()
                .map(OccurrenceResponse::dayOfMonth)
                .toList();
    }

    private UpdateTaskRequest updateRequest(
            Long categoryId,
            RecurrenceType recurrenceType,
            TaskRuleRequest rule
    ) {
        return updateRequest(categoryId, recurrenceType, true, rule);
    }

    private UpdateTaskRequest updateRequest(
            Long categoryId,
            RecurrenceType recurrenceType,
            Boolean isActive,
            TaskRuleRequest rule
    ) {
        return new UpdateTaskRequest(
                categoryId,
                "Edited Task",
                "Updated",
                recurrenceType,
                LocalDate.of(2026, 4, 1),
                null,
                isActive,
                null,
                null,
                rule
        );
    }

    private UpdateTaskRequest scopedUpdateRequest(
            TaskEditScope editScope,
            LocalDate selectedOccurrenceDate,
            Long categoryId,
            RecurrenceType recurrenceType,
            TaskRuleRequest rule
    ) {
        return new UpdateTaskRequest(
                categoryId,
                "Edited Task",
                "Updated",
                recurrenceType,
                LocalDate.of(2026, 4, 1),
                null,
                true,
                editScope,
                selectedOccurrenceDate,
                rule
        );
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        ReflectionTestUtils.setField(category, "id", id);
        category.setName(name);
        return category;
    }

    private Task fixedDateTask(Long id, Category category, int dayOfMonth) {
        Task createdTask = new Task();
        ReflectionTestUtils.setField(createdTask, "id", id);
        createdTask.setCategory(category);
        createdTask.setName("Editable Task");
        createdTask.setDescription("Before edit");
        createdTask.setRecurrenceType(RecurrenceType.FIXED_DATE);
        createdTask.setStartDate(LocalDate.of(2026, 4, 1));
        createdTask.setActive(true);

        TaskRecurrenceRule rule = new TaskRecurrenceRule();
        rule.setFallbackToLastDay(true);
        createdTask.setRecurrenceRule(rule);

        TaskFixedDate fixedDate = new TaskFixedDate();
        fixedDate.setDayOfMonth(dayOfMonth);
        createdTask.addFixedDate(fixedDate);

        return createdTask;
    }
}
