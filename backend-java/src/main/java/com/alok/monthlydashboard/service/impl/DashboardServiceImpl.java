package com.alok.monthlydashboard.service.impl;

import com.alok.monthlydashboard.dto.dashboard.DashboardCategoryResponse;
import com.alok.monthlydashboard.dto.dashboard.DashboardTaskResponse;
import com.alok.monthlydashboard.dto.dashboard.DayStripItemResponse;
import com.alok.monthlydashboard.dto.dashboard.MonthlyDashboardResponse;
import com.alok.monthlydashboard.dto.dashboard.OccurrenceResponse;
import com.alok.monthlydashboard.dto.dashboard.ScaleBarResponse;
import com.alok.monthlydashboard.entity.Category;
import com.alok.monthlydashboard.entity.Task;
import com.alok.monthlydashboard.repository.CategoryRepository;
import com.alok.monthlydashboard.repository.TaskRepository;
import com.alok.monthlydashboard.service.DashboardService;
import com.alok.monthlydashboard.service.RecurrenceService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final CategoryRepository categoryRepository;
    private final TaskRepository taskRepository;
    private final RecurrenceService recurrenceService;

    public DashboardServiceImpl(
            CategoryRepository categoryRepository,
            TaskRepository taskRepository,
            RecurrenceService recurrenceService
    ) {
        this.categoryRepository = categoryRepository;
        this.taskRepository = taskRepository;
        this.recurrenceService = recurrenceService;
    }

    @Override
    public MonthlyDashboardResponse getMonthlyDashboard(int year, int month) {
        YearMonth targetMonth = YearMonth.of(year, month);
        LocalDate today = LocalDate.now();

        ScaleBarResponse scaleBar = buildScaleBar(targetMonth, today);
        List<DayStripItemResponse> dayStrip = buildDayStrip(targetMonth, today);
        List<DashboardCategoryResponse> categoryResponses = buildCategoryResponses(targetMonth);

        return new MonthlyDashboardResponse(
                year,
                month,
                buildMonthLabel(targetMonth),
                today,
                false,
                scaleBar,
                dayStrip,
                categoryResponses
        );
    }

    private List<DashboardCategoryResponse> buildCategoryResponses(YearMonth targetMonth) {
        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
        List<DashboardCategoryResponse> results = new ArrayList<>();

        for (Category category : categories) {
            List<Task> tasks = taskRepository.findByCategoryIdAndIsActiveOrderByNameAsc(
                    category.getId(),
                    true
            );

            List<DashboardTaskResponse> taskResponses = new ArrayList<>();

            for (Task task : tasks) {
                List<OccurrenceResponse> occurrences = recurrenceService.generateOccurrencesForMonth(
                        task.getId(),
                        targetMonth.getYear(),
                        targetMonth.getMonthValue()
                );

                taskResponses.add(new DashboardTaskResponse(
                        task.getId(),
                        task.getName(),
                        task.getRecurrenceType(),
                        occurrences
                ));
            }

            results.add(new DashboardCategoryResponse(
                    category.getId(),
                    category.getName(),
                    taskResponses
            ));
        }

        return results;
    }

    private ScaleBarResponse buildScaleBar(YearMonth targetMonth, LocalDate today) {
        int lastDay = targetMonth.lengthOfMonth();
        List<Integer> anchors = List.of(1, 8, 15, 22, lastDay);

        String currentDateLabel;
        if (YearMonth.from(today).equals(targetMonth)) {
            currentDateLabel = today.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    + " "
                    + today.getDayOfMonth();
        } else {
            currentDateLabel = targetMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    + " 1";
        }

        return new ScaleBarResponse(
                anchors,
                lastDay,
                currentDateLabel
        );
    }

    private List<DayStripItemResponse> buildDayStrip(YearMonth targetMonth, LocalDate today) {
        List<DayStripItemResponse> items = new ArrayList<>();

        for (int day = 1; day <= targetMonth.lengthOfMonth(); day++) {
            LocalDate date = targetMonth.atDay(day);
            DayOfWeek dayOfWeek = date.getDayOfWeek();

            items.add(new DayStripItemResponse(
                    date,
                    day,
                    dayOfWeek.name(),
                    date.equals(today),
                    isWeekend(dayOfWeek)
            ));
        }

        return items;
    }

    private boolean isWeekend(DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private String buildMonthLabel(YearMonth targetMonth) {
        return targetMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " "
                + targetMonth.getYear();
    }
}