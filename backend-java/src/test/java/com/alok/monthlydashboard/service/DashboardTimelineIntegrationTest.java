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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/dashboard-timeline-integration-test.db",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class DashboardTimelineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCompletionRepository taskCompletionRepository;

    @MockBean
    private AppDateProvider appDateProvider;

    private final LocalDate today = LocalDate.of(2026, 5, 15);

    @BeforeEach
    void setUp() {
        when(appDateProvider.today()).thenReturn(today);

        taskCompletionRepository.deleteAll();
        taskRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = new Category();
        category.setName("Home");
        category.setColor("#2563eb");
        Category savedCategory = categoryRepository.save(category);

        Task task = new Task();
        task.setCategory(savedCategory);
        task.setName("Water Plants");
        task.setDescription("");
        task.setRecurrenceType(RecurrenceType.FIXED_DATE);
        task.setStartDate(LocalDate.of(2026, 5, 1));
        task.setActive(true);

        TaskRecurrenceRule rule = new TaskRecurrenceRule();
        rule.setFallbackToLastDay(true);
        task.setRecurrenceRule(rule);

        TaskFixedDate dueToday = new TaskFixedDate();
        dueToday.setDayOfMonth(15);
        task.addFixedDate(dueToday);

        TaskFixedDate sameWeekWeekday = new TaskFixedDate();
        sameWeekWeekday.setDayOfMonth(13);
        task.addFixedDate(sameWeekWeekday);

        TaskFixedDate sameWeekWeekend = new TaskFixedDate();
        sameWeekWeekend.setDayOfMonth(16);
        task.addFixedDate(sameWeekWeekend);

        TaskFixedDate upcoming = new TaskFixedDate();
        upcoming.setDayOfMonth(20);
        task.addFixedDate(upcoming);

        Task savedTask = taskRepository.save(task);

        TaskCompletion completion = new TaskCompletion();
        completion.setTask(savedTask);
        completion.setOccurrenceDate(today);
        completion.setCompletionDate(today);
        taskCompletionRepository.save(completion);
    }

    @Test
    void monthlyDashboardEndpointStillUsesRequestedMonth() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/monthly")
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(4))
                .andExpect(jsonPath("$.monthLabel").value("April 2026"))
                .andExpect(jsonPath("$.dayStrip", hasSize(30)));
    }

    @Test
    void timelineDashboardDefaultsToCurrentMonthFromAppDateProvider() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.view").value("MONTH"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(5))
                .andExpect(jsonPath("$.startDate").value("2026-05-01"))
                .andExpect(jsonPath("$.endDate").value("2026-05-31"))
                .andExpect(jsonPath("$.label").value("May 2026"))
                .andExpect(jsonPath("$.today").value("2026-05-15"))
                .andExpect(jsonPath("$.settings.view").value("MONTH"))
                .andExpect(jsonPath("$.settings.startOfWeek").value("SUNDAY"))
                .andExpect(jsonPath("$.settings.scaleNumbering").value("SEGMENT"))
                .andExpect(jsonPath("$.settings.calendarYearBound").value(true));
    }

    @Test
    void timelineMonthReturnsOneDayCellPerCurrentMonthDay() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cells", hasSize(31)))
                .andExpect(jsonPath("$.cells[0].key").value("2026-05-01"))
                .andExpect(jsonPath("$.cells[0].cellType").value("DAY"))
                .andExpect(jsonPath("$.cells[0].label").value("1"))
                .andExpect(jsonPath("$.cells[14].key").value("2026-05-15"))
                .andExpect(jsonPath("$.cells[14].isToday").value(true))
                .andExpect(jsonPath("$.cells[30].key").value("2026-05-31"));
    }

    @Test
    void timelineIncludesCategoriesTasksAndOccurrenceBuckets() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(1)))
                .andExpect(jsonPath("$.categories[0].categoryName").value("Home"))
                .andExpect(jsonPath("$.categories[0].tasks", hasSize(1)))
                .andExpect(jsonPath("$.categories[0].tasks[0].taskName").value("Water Plants"))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets", hasSize(31)));
    }

    @Test
    void timelineBucketLabelsUseCompletedAndTotalOccurrenceCounts() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[14].cellKey").value("2026-05-15"))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[14].totalOccurrences").value(1))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[14].completedOccurrences").value(1))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[14].completionLabel").value("1/1"))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[14].collapsedLabel").value("x1"))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[19].cellKey").value("2026-05-20"))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[19].totalOccurrences").value(1))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[19].completedOccurrences").value(0))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[19].completionLabel").value("0/1"))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[19].collapsedLabel").value("x1"));
    }

    @Test
    void unsupportedTimelineViewsReturnControlledValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/timeline")
                        .param("view", "YEAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Timeline view YEAR is not supported yet"));
    }

    @Test
    void quarterWithCalendarYearBoundReturnsCurrentCalendarQuarter() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/timeline")
                        .param("view", "QUARTER")
                        .param("calendarYearBound", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.view").value("QUARTER"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(4))
                .andExpect(jsonPath("$.startDate").value("2026-04-01"))
                .andExpect(jsonPath("$.endDate").value("2026-06-30"))
                .andExpect(jsonPath("$.label").value("Q2 2026"))
                .andExpect(jsonPath("$.settings.calendarYearBound").value(true));
    }

    @Test
    void quarterWithoutCalendarYearBoundReturnsRollingThreeMonthRange() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/timeline")
                        .param("view", "QUARTER")
                        .param("calendarYearBound", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.view").value("QUARTER"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(5))
                .andExpect(jsonPath("$.startDate").value("2026-05-01"))
                .andExpect(jsonPath("$.endDate").value("2026-07-31"))
                .andExpect(jsonPath("$.label").value("May-Jul 2026"))
                .andExpect(jsonPath("$.settings.calendarYearBound").value(false));
    }

    @Test
    void quarterGeneratesWeekdayAndWeekendBucketsUsingSundayWeekStart() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/timeline")
                        .param("view", "QUARTER")
                        .param("calendarYearBound", "true")
                        .param("startOfWeek", "SUNDAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cells", hasSize(28)))
                .andExpect(jsonPath("$.cells[0].key").value("2026-03-29-WEEKDAY_BUCKET"))
                .andExpect(jsonPath("$.cells[0].startDate").value("2026-04-01"))
                .andExpect(jsonPath("$.cells[0].endDate").value("2026-04-04"))
                .andExpect(jsonPath("$.cells[0].cellType").value("WEEKDAY_BUCKET"))
                .andExpect(jsonPath("$.cells[1].key").value("2026-03-29-WEEKEND_BUCKET"))
                .andExpect(jsonPath("$.cells[1].cellType").value("WEEKEND_BUCKET"));
    }

    @Test
    void quarterHonoursMondayWeekStartWhenGeneratingBuckets() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/timeline")
                        .param("view", "QUARTER")
                        .param("calendarYearBound", "true")
                        .param("startOfWeek", "MONDAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cells", hasSize(28)))
                .andExpect(jsonPath("$.cells[0].key").value("2026-03-30-WEEKDAY_BUCKET"))
                .andExpect(jsonPath("$.cells[0].startDate").value("2026-04-01"))
                .andExpect(jsonPath("$.cells[0].endDate").value("2026-04-05"))
                .andExpect(jsonPath("$.cells[0].cellType").value("WEEKDAY_BUCKET"))
                .andExpect(jsonPath("$.cells[1].key").value("2026-03-30-WEEKEND_BUCKET"))
                .andExpect(jsonPath("$.cells[1].cellType").value("WEEKEND_BUCKET"));
    }

    @Test
    void quarterMapsOccurrencesIntoWeekdayAndWeekendBuckets() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/timeline")
                        .param("view", "QUARTER")
                        .param("calendarYearBound", "false")
                        .param("startOfWeek", "SUNDAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[4].cellKey")
                        .value("2026-05-10-WEEKDAY_BUCKET"))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[4].totalOccurrences").value(2))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[4].completedOccurrences").value(1))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[4].completionLabel").value("1/2"))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[4].collapsedLabel").value("x2"))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[4].occurrences", hasSize(2)))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[5].cellKey")
                        .value("2026-05-10-WEEKEND_BUCKET"))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[5].totalOccurrences").value(1))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[5].completedOccurrences").value(0))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[5].completionLabel").value("0/1"))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[5].collapsedLabel").value("x1"))
                .andExpect(jsonPath("$.categories[0].tasks[0].buckets[5].occurrences", hasSize(1)));
    }
}
