package com.alok.monthlydashboard.service;

import com.alok.monthlydashboard.common.enums.OccurrenceStatus;
import com.alok.monthlydashboard.common.enums.RecurrenceType;
import com.alok.monthlydashboard.dto.checklist.TodayChecklistResponse;
import com.alok.monthlydashboard.entity.Category;
import com.alok.monthlydashboard.entity.Task;
import com.alok.monthlydashboard.entity.TaskCompletion;
import com.alok.monthlydashboard.entity.TaskFixedDate;
import com.alok.monthlydashboard.repository.CategoryRepository;
import com.alok.monthlydashboard.repository.TaskCompletionRepository;
import com.alok.monthlydashboard.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/checklist-integration-test.db",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class ChecklistIntegrationTest {

    @Autowired
    private ChecklistService checklistService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCompletionRepository taskCompletionRepository;

    @MockBean
    private AppDateProvider appDateProvider;

    private Category category;
    private final LocalDate today = LocalDate.of(2026, 4, 28);

    @BeforeEach
    void setUp() {
        when(appDateProvider.today()).thenReturn(today);

        taskCompletionRepository.deleteAll();
        taskRepository.deleteAll();
        categoryRepository.deleteAll();

        Category createdCategory = new Category();
        createdCategory.setName("Home");
        createdCategory.setColor("#2563eb");
        category = categoryRepository.save(createdCategory);
    }

    @Test
    void dueTodayIncompleteAppearsInTodayChecklist() {
        Task task = saveFixedDateTask("Flowers", today, today.getDayOfMonth());

        TodayChecklistResponse checklist = checklistService.getTodayChecklist();

        assertThat(checklist.today()).isEqualTo(today);
        assertThat(checklist.items())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.taskId()).isEqualTo(task.getId());
                    assertThat(item.taskName()).isEqualTo("Flowers");
                    assertThat(item.occurrenceDate()).isEqualTo(today);
                    assertThat(item.status()).isEqualTo(OccurrenceStatus.DUE_TODAY);
                });
    }

    @Test
    void overdueIncompleteAppearsInTodayChecklist() {
        LocalDate overdueDate = today.minusDays(1);
        Task task = saveFixedDateTask("Movie Night", overdueDate, overdueDate.getDayOfMonth());

        TodayChecklistResponse checklist = checklistService.getTodayChecklist();

        assertThat(checklist.items())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.taskId()).isEqualTo(task.getId());
                    assertThat(item.taskName()).isEqualTo("Movie Night");
                    assertThat(item.occurrenceDate()).isEqualTo(overdueDate);
                    assertThat(item.status()).isEqualTo(OccurrenceStatus.OVERDUE);
                });
    }

    @Test
    void dueTodayCompletedAppearsInTodayChecklist() {
        Task task = saveFixedDateTask("Done Today", today, today.getDayOfMonth());
        saveCompletion(task, today, today);

        TodayChecklistResponse checklist = checklistService.getTodayChecklist();

        assertThat(checklist.items())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.taskId()).isEqualTo(task.getId());
                    assertThat(item.taskName()).isEqualTo("Done Today");
                    assertThat(item.occurrenceDate()).isEqualTo(today);
                    assertThat(item.status()).isEqualTo(OccurrenceStatus.COMPLETED);
                });
    }

    @Test
    void overdueCompletedYesterdayDoesNotAppearInTodayChecklist() {
        LocalDate completedOccurrenceDate = today.minusDays(1);
        Task task = saveFixedDateTask(
                "Already Done",
                completedOccurrenceDate,
                completedOccurrenceDate.getDayOfMonth()
        );
        saveCompletion(task, completedOccurrenceDate, completedOccurrenceDate);

        TodayChecklistResponse checklist = checklistService.getTodayChecklist();

        assertThat(checklist.items()).isEmpty();
    }

    @Test
    void overdueCompletedTodayDoesNotAppearInTodayChecklist() {
        LocalDate overdueOccurrenceDate = today.minusDays(1);
        Task task = saveFixedDateTask(
                "Late Done Today",
                overdueOccurrenceDate,
                overdueOccurrenceDate.getDayOfMonth()
        );
        saveCompletion(task, overdueOccurrenceDate, today);

        TodayChecklistResponse checklist = checklistService.getTodayChecklist();

        assertThat(checklist.items()).isEmpty();
    }

    @Test
    void futureOccurrencesDoNotAppearInTodayChecklist() {
        LocalDate futureDate = today.plusDays(1);
        saveFixedDateTask("Future Task", today, futureDate.getDayOfMonth());

        TodayChecklistResponse checklist = checklistService.getTodayChecklist();

        assertThat(checklist.items()).isEmpty();
    }

    private Task saveFixedDateTask(String name, LocalDate startDate, int dayOfMonth) {
        Task task = new Task();
        task.setCategory(category);
        task.setName(name);
        task.setDescription("");
        task.setRecurrenceType(RecurrenceType.FIXED_DATE);
        task.setStartDate(startDate);
        task.setActive(true);

        TaskFixedDate fixedDate = new TaskFixedDate();
        fixedDate.setDayOfMonth(dayOfMonth);
        task.addFixedDate(fixedDate);

        return taskRepository.save(task);
    }

    private void saveCompletion(Task task, LocalDate occurrenceDate, LocalDate completionDate) {
        TaskCompletion completion = new TaskCompletion();
        completion.setTask(task);
        completion.setOccurrenceDate(occurrenceDate);
        completion.setCompletionDate(completionDate);
        taskCompletionRepository.save(completion);
    }
}
