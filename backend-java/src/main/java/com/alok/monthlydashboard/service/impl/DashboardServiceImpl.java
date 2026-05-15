package com.alok.monthlydashboard.service.impl;

import com.alok.monthlydashboard.common.enums.ScaleNumbering;
import com.alok.monthlydashboard.common.enums.StartOfWeek;
import com.alok.monthlydashboard.common.enums.TimelineCellType;
import com.alok.monthlydashboard.common.enums.TimelineView;
import com.alok.monthlydashboard.dto.dashboard.DashboardCategoryResponse;
import com.alok.monthlydashboard.dto.dashboard.DashboardTaskResponse;
import com.alok.monthlydashboard.dto.dashboard.DayStripItemResponse;
import com.alok.monthlydashboard.dto.dashboard.MonthlyDashboardResponse;
import com.alok.monthlydashboard.dto.dashboard.OccurrenceResponse;
import com.alok.monthlydashboard.dto.dashboard.ScaleBarResponse;
import com.alok.monthlydashboard.dto.dashboard.TimelineCategoryResponse;
import com.alok.monthlydashboard.dto.dashboard.TimelineCellResponse;
import com.alok.monthlydashboard.dto.dashboard.TimelineDashboardResponse;
import com.alok.monthlydashboard.dto.dashboard.TimelineOccurrenceBucketResponse;
import com.alok.monthlydashboard.dto.dashboard.TimelineOccurrenceResponse;
import com.alok.monthlydashboard.dto.dashboard.TimelineScaleBarResponse;
import com.alok.monthlydashboard.dto.dashboard.TimelineSettingsResponse;
import com.alok.monthlydashboard.dto.dashboard.TimelineTaskResponse;
import com.alok.monthlydashboard.entity.Category;
import com.alok.monthlydashboard.entity.Task;
import com.alok.monthlydashboard.exception.ValidationException;
import com.alok.monthlydashboard.repository.CategoryRepository;
import com.alok.monthlydashboard.repository.TaskRepository;
import com.alok.monthlydashboard.service.AppDateProvider;
import com.alok.monthlydashboard.service.DashboardService;
import com.alok.monthlydashboard.service.RecurrenceService;
import com.alok.monthlydashboard.util.RecurrenceSummaryHelper;
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
    private final AppDateProvider appDateProvider;

    public DashboardServiceImpl(
            CategoryRepository categoryRepository,
            TaskRepository taskRepository,
            RecurrenceService recurrenceService,
            AppDateProvider appDateProvider
    ) {
        this.categoryRepository = categoryRepository;
        this.taskRepository = taskRepository;
        this.recurrenceService = recurrenceService;
        this.appDateProvider = appDateProvider;
    }

    @Override
    public MonthlyDashboardResponse getMonthlyDashboard(int year, int month) {
        YearMonth targetMonth = YearMonth.of(year, month);
        LocalDate today = appDateProvider.today();

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

    @Override
    public TimelineDashboardResponse getTimelineDashboard(
            TimelineView view,
            StartOfWeek startOfWeek,
            ScaleNumbering scaleNumbering,
            boolean calendarYearBound
    ) {
        if (view != TimelineView.MONTH) {
            throw new ValidationException("Timeline view " + view + " is not supported yet");
        }

        LocalDate today = appDateProvider.today();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();
        List<TimelineCellResponse> cells = buildTimelineDayCells(currentMonth, today);

        TimelineSettingsResponse settings = new TimelineSettingsResponse(
                view,
                startOfWeek,
                scaleNumbering,
                calendarYearBound
        );

        return new TimelineDashboardResponse(
                view,
                currentMonth.getYear(),
                currentMonth.getMonthValue(),
                startDate,
                endDate,
                buildMonthLabel(currentMonth),
                today,
                false,
                settings,
                buildTimelineScaleBar(currentMonth, today),
                cells,
                buildTimelineCategoryResponses(currentMonth, cells)
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
                        RecurrenceSummaryHelper.summarize(task),
                        occurrences
                ));
            }

            results.add(new DashboardCategoryResponse(
                    category.getId(),
                    category.getName(),
                    category.getColor(),
                    taskResponses
            ));
        }

        return results;
    }

    private List<TimelineCategoryResponse> buildTimelineCategoryResponses(
            YearMonth targetMonth,
            List<TimelineCellResponse> cells
    ) {
        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
        List<TimelineCategoryResponse> results = new ArrayList<>();

        for (Category category : categories) {
            List<Task> tasks = taskRepository.findByCategoryIdAndIsActiveOrderByNameAsc(
                    category.getId(),
                    true
            );

            List<TimelineTaskResponse> taskResponses = new ArrayList<>();

            for (Task task : tasks) {
                List<OccurrenceResponse> occurrences = recurrenceService.generateOccurrencesForMonth(
                        task.getId(),
                        targetMonth.getYear(),
                        targetMonth.getMonthValue()
                );

                taskResponses.add(new TimelineTaskResponse(
                        task.getId(),
                        task.getName(),
                        task.getRecurrenceType(),
                        RecurrenceSummaryHelper.summarize(task),
                        buildTimelineOccurrenceBuckets(cells, occurrences)
                ));
            }

            results.add(new TimelineCategoryResponse(
                    category.getId(),
                    category.getName(),
                    category.getColor(),
                    taskResponses
            ));
        }

        return results;
    }

    private List<TimelineOccurrenceBucketResponse> buildTimelineOccurrenceBuckets(
            List<TimelineCellResponse> cells,
            List<OccurrenceResponse> occurrences
    ) {
        List<TimelineOccurrenceBucketResponse> buckets = new ArrayList<>();

        for (TimelineCellResponse cell : cells) {
            List<TimelineOccurrenceResponse> bucketOccurrences = occurrences.stream()
                    .filter(occurrence -> !occurrence.occurrenceDate().isBefore(cell.startDate())
                            && !occurrence.occurrenceDate().isAfter(cell.endDate()))
                    .map(this::toTimelineOccurrence)
                    .toList();

            int totalOccurrences = bucketOccurrences.size();
            int completedOccurrences = (int) bucketOccurrences.stream()
                    .filter(TimelineOccurrenceResponse::completed)
                    .count();

            buckets.add(new TimelineOccurrenceBucketResponse(
                    cell.key(),
                    totalOccurrences,
                    completedOccurrences,
                    completedOccurrences + "/" + totalOccurrences,
                    "x" + totalOccurrences,
                    bucketOccurrences
            ));
        }

        return buckets;
    }

    private TimelineOccurrenceResponse toTimelineOccurrence(OccurrenceResponse occurrence) {
        return new TimelineOccurrenceResponse(
                occurrence.occurrenceDate(),
                occurrence.completed(),
                occurrence.completionDate(),
                occurrence.status()
        );
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

    private TimelineScaleBarResponse buildTimelineScaleBar(YearMonth targetMonth, LocalDate today) {
        int lastDay = targetMonth.lengthOfMonth();
        List<String> anchorCellKeys = List.of(1, 8, 15, 22, lastDay)
                .stream()
                .map(day -> cellKey(targetMonth.atDay(day)))
                .toList();

        String currentDateLabel = today.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                + " "
                + today.getDayOfMonth();

        return new TimelineScaleBarResponse(anchorCellKeys, currentDateLabel);
    }

    private List<TimelineCellResponse> buildTimelineDayCells(YearMonth targetMonth, LocalDate today) {
        List<TimelineCellResponse> cells = new ArrayList<>();

        for (int day = 1; day <= targetMonth.lengthOfMonth(); day++) {
            LocalDate date = targetMonth.atDay(day);
            DayOfWeek dayOfWeek = date.getDayOfWeek();

            cells.add(new TimelineCellResponse(
                    cellKey(date),
                    date,
                    date,
                    String.valueOf(day),
                    dayOfWeek.name(),
                    TimelineCellType.DAY,
                    day,
                    day,
                    date.equals(today),
                    isWeekend(dayOfWeek),
                    true
            ));
        }

        return cells;
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

    private String cellKey(LocalDate date) {
        return date.toString();
    }
}
