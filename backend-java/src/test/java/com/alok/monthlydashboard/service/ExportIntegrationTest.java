package com.alok.monthlydashboard.service;

import com.alok.monthlydashboard.common.enums.RecurrenceType;
import com.alok.monthlydashboard.entity.Category;
import com.alok.monthlydashboard.entity.Task;
import com.alok.monthlydashboard.entity.TaskCompletion;
import com.alok.monthlydashboard.entity.TaskFixedDate;
import com.alok.monthlydashboard.entity.TaskRecurrenceRule;
import com.alok.monthlydashboard.repository.CategoryRepository;
import com.alok.monthlydashboard.repository.TaskCompletionRepository;
import com.alok.monthlydashboard.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/export-integration-test.db",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class ExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCompletionRepository taskCompletionRepository;

    private Category category;
    private Task task;

    @BeforeEach
    void setUp() {
        taskCompletionRepository.deleteAll();
        taskRepository.deleteAll();
        categoryRepository.deleteAll();

        Category createdCategory = new Category();
        createdCategory.setName("Home");
        createdCategory.setColor("#2563eb");
        category = categoryRepository.save(createdCategory);

        task = taskRepository.save(fixedDateTask());

        TaskCompletion completion = new TaskCompletion();
        completion.setTask(task);
        completion.setOccurrenceDate(LocalDate.of(2026, 4, 15));
        completion.setCompletionDate(LocalDate.of(2026, 4, 16));
        taskCompletionRepository.save(completion);
    }

    @Test
    void exportsCompleteSystemState() throws Exception {
        mockMvc.perform(get("/api/v1/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(1)))
                .andExpect(jsonPath("$.categories[0].id").value(category.getId()))
                .andExpect(jsonPath("$.categories[0].name").value("Home"))
                .andExpect(jsonPath("$.categories[0].color").value("#2563eb"))
                .andExpect(jsonPath("$.tasks", hasSize(1)))
                .andExpect(jsonPath("$.tasks[0].id").value(task.getId()))
                .andExpect(jsonPath("$.tasks[0].categoryId").value(category.getId()))
                .andExpect(jsonPath("$.tasks[0].name").value("Flowers"))
                .andExpect(jsonPath("$.tasks[0].recurrenceType").value("FIXED_DATE"))
                .andExpect(jsonPath("$.tasks[0].startDate").value("2026-04-01"))
                .andExpect(jsonPath("$.tasks[0].active").value(true))
                .andExpect(jsonPath("$.recurrenceRules", hasSize(1)))
                .andExpect(jsonPath("$.recurrenceRules[0].taskId").value(task.getId()))
                .andExpect(jsonPath("$.recurrenceRules[0].fallbackToLastDay").value(true))
                .andExpect(jsonPath("$.recurrenceRules[0].fixedDates", hasSize(1)))
                .andExpect(jsonPath("$.recurrenceRules[0].fixedDates[0].dayOfMonth").value(15))
                .andExpect(jsonPath("$.completionHistory", hasSize(1)))
                .andExpect(jsonPath("$.completionHistory[0].taskId").value(task.getId()))
                .andExpect(jsonPath("$.completionHistory[0].occurrenceDate").value("2026-04-15"))
                .andExpect(jsonPath("$.completionHistory[0].completionDate").value("2026-04-16"));
    }

    private Task fixedDateTask() {
        Task createdTask = new Task();
        createdTask.setCategory(category);
        createdTask.setName("Flowers");
        createdTask.setDescription("Water flowers");
        createdTask.setRecurrenceType(RecurrenceType.FIXED_DATE);
        createdTask.setStartDate(LocalDate.of(2026, 4, 1));
        createdTask.setActive(true);

        TaskRecurrenceRule rule = new TaskRecurrenceRule();
        rule.setFallbackToLastDay(true);
        createdTask.setRecurrenceRule(rule);

        TaskFixedDate fixedDate = new TaskFixedDate();
        fixedDate.setDayOfMonth(15);
        createdTask.addFixedDate(fixedDate);

        return createdTask;
    }
}
