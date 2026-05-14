package com.alok.monthlydashboard.service;

import com.alok.monthlydashboard.common.enums.RecurrenceType;
import com.alok.monthlydashboard.entity.Category;
import com.alok.monthlydashboard.entity.Task;
import com.alok.monthlydashboard.repository.CategoryRepository;
import com.alok.monthlydashboard.repository.TaskCompletionRepository;
import com.alok.monthlydashboard.repository.TaskFixedDateRepository;
import com.alok.monthlydashboard.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/setup-import-integration-test.db",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class SetupImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCompletionRepository taskCompletionRepository;

    @Autowired
    private TaskFixedDateRepository taskFixedDateRepository;

    @BeforeEach
    void setUp() {
        taskCompletionRepository.deleteAll();
        taskRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void previewsSetupSnapshotWithoutWritingToDatabase() throws Exception {
        mockMvc.perform(post("/api/v1/import/setup/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSnapshot()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.version").value("v2-setup-snapshot"))
                .andExpect(jsonPath("$.categoryCount").value(1))
                .andExpect(jsonPath("$.taskCount").value(2))
                .andExpect(jsonPath("$.activeTaskCount").value(1))
                .andExpect(jsonPath("$.inactiveTaskCount").value(1))
                .andExpect(jsonPath("$.warnings", hasSize(0)))
                .andExpect(jsonPath("$.errors", hasSize(0)));

        assertThat(categoryRepository.count()).isZero();
        assertThat(taskRepository.count()).isZero();
    }

    @Test
    void importsSetupSnapshotIntoEmptySetup() throws Exception {
        mockMvc.perform(post("/api/v1/import/setup?mode=EMPTY_ONLY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSnapshot()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(true))
                .andExpect(jsonPath("$.mode").value("EMPTY_ONLY"))
                .andExpect(jsonPath("$.categoryCount").value(1))
                .andExpect(jsonPath("$.taskCount").value(2));

        assertThat(categoryRepository.count()).isEqualTo(1);
        assertThat(taskRepository.count()).isEqualTo(2);
        assertThat(taskCompletionRepository.count()).isZero();

        Category category = categoryRepository.findAll().get(0);
        assertThat(category.getName()).isEqualTo("Finance");
        assertThat(category.getRequires().name()).isEqualTo("FOCUS");

        Task activeTask = taskRepository.findAllByOrderByNameAsc().get(0);
        assertThat(activeTask.getCategory().getId()).isEqualTo(category.getId());
        assertThat(activeTask.getRecurrenceType()).isEqualTo(RecurrenceType.FIXED_DATE);
        assertThat(activeTask.isActive()).isTrue();
        assertThat(activeTask.getRecurrenceRule().isFallbackToLastDay()).isTrue();
        assertThat(taskFixedDateRepository.findByTaskIdOrderByDayOfMonthAsc(activeTask.getId()))
                .extracting("dayOfMonth")
                .containsExactly(12, 31);

        Task inactiveTask = taskRepository.findAllByOrderByNameAsc().get(1);
        assertThat(inactiveTask.isActive()).isFalse();
    }

    @Test
    void importIntoNonEmptySetupIsBlocked() throws Exception {
        Category category = new Category();
        category.setName("Existing");
        category.setColor("#2563eb");
        categoryRepository.save(category);

        mockMvc.perform(post("/api/v1/import/setup?mode=EMPTY_ONLY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSnapshot()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Setup import requires an empty setup. Existing categories or tasks were found."));
    }

    @Test
    void previewReportsInvalidSnapshotErrorsAndTaskCountWarning() throws Exception {
        mockMvc.perform(post("/api/v1/import/setup/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidSnapshot()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.warnings", hasItem("categories[100].taskCount does not match referenced tasks and will be ignored.")))
                .andExpect(jsonPath("$.errors", hasItem("tasks[0].categoryId must reference a snapshot category.")))
                .andExpect(jsonPath("$.errors", hasItem("tasks[0].recurrenceType is invalid: NEVER.")))
                .andExpect(jsonPath("$.errors", hasItem("tasks[0].startDate must be a valid ISO date.")));
    }

    private String validSnapshot() {
        return """
                {
                  "exportedAt": "2026-05-14T11:30:00",
                  "version": "v2-setup-snapshot",
                  "categories": [
                    {
                      "id": 100,
                      "name": "Finance",
                      "color": "#2563eb",
                      "requires": "FOCUS",
                      "feelsLike": ["QUICK_WIN"],
                      "taskCount": 2
                    }
                  ],
                  "tasks": [
                    {
                      "id": 200,
                      "categoryId": 100,
                      "categoryName": "Finance",
                      "name": "Credit Card Payment",
                      "description": "Pay monthly credit card bill",
                      "recurrenceType": "FIXED_DATE",
                      "startDate": "2026-05-14",
                      "endDate": "2027-05-14",
                      "isActive": true,
                      "rule": {
                        "fixedDates": [12, 31],
                        "fallbackToLastDay": null
                      },
                      "completionHistory": [
                        { "taskId": 200, "occurrenceDate": "2026-05-12" }
                      ]
                    },
                    {
                      "id": 201,
                      "categoryId": 100,
                      "categoryName": "Finance",
                      "name": "Inactive Audit",
                      "description": "",
                      "recurrenceType": "INTERVAL",
                      "startDate": "2026-05-14",
                      "endDate": null,
                      "isActive": false,
                      "rule": {
                        "intervalValue": 2,
                        "intervalUnit": "WEEKS"
                      }
                    }
                  ]
                }
                """;
    }

    private String invalidSnapshot() {
        return """
                {
                  "version": "v2-setup-snapshot",
                  "categories": [
                    {
                      "id": 100,
                      "name": "Finance",
                      "taskCount": 1
                    }
                  ],
                  "tasks": [
                    {
                      "id": 200,
                      "categoryId": 999,
                      "name": "Bad Task",
                      "recurrenceType": "NEVER",
                      "startDate": "not-a-date",
                      "rule": {}
                    }
                  ]
                }
                """;
    }
}
