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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
        if (view != TimelineView.MONTH
                && view != TimelineView.QUARTER
                && view != TimelineView.QUADRIMESTER
                && view != TimelineView.HALF_YEAR
                && view != TimelineView.YEAR) {
            throw new ValidationException("Timeline view " + view + " is not supported yet");
        }

        LocalDate today = appDateProvider.today();

        if (view == TimelineView.YEAR) {
            return getYearTimelineDashboard(today, startOfWeek, scaleNumbering, calendarYearBound);
        }

        if (view == TimelineView.HALF_YEAR) {
            return getHalfYearTimelineDashboard(today, startOfWeek, scaleNumbering, calendarYearBound);
        }

        if (view == TimelineView.QUADRIMESTER) {
            return getQuadrimesterTimelineDashboard(today, startOfWeek, scaleNumbering, calendarYearBound);
        }

        if (view == TimelineView.QUARTER) {
            return getQuarterTimelineDashboard(today, startOfWeek, scaleNumbering, calendarYearBound);
        }

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

    private TimelineDashboardResponse getQuarterTimelineDashboard(
            LocalDate today,
            StartOfWeek startOfWeek,
            ScaleNumbering scaleNumbering,
            boolean calendarYearBound
    ) {
        TimelineDateRange dateRange = buildQuarterDateRange(today, calendarYearBound);
        List<TimelineCellResponse> cells = buildWeekBucketCells(
                dateRange.startDate(),
                dateRange.endDate(),
                today,
                startOfWeek
        );

        TimelineSettingsResponse settings = new TimelineSettingsResponse(
                TimelineView.QUARTER,
                startOfWeek,
                scaleNumbering,
                calendarYearBound
        );

        return new TimelineDashboardResponse(
                TimelineView.QUARTER,
                dateRange.startDate().getYear(),
                dateRange.startDate().getMonthValue(),
                dateRange.startDate(),
                dateRange.endDate(),
                dateRange.label(),
                today,
                false,
                settings,
                buildTimelineScaleBar(dateRange.startDate(), dateRange.endDate(), cells, today),
                cells,
                buildTimelineCategoryResponses(dateRange.startDate(), dateRange.endDate(), cells)
        );
    }

    private TimelineDashboardResponse getQuadrimesterTimelineDashboard(
            LocalDate today,
            StartOfWeek startOfWeek,
            ScaleNumbering scaleNumbering,
            boolean calendarYearBound
    ) {
        TimelineDateRange dateRange = buildQuadrimesterDateRange(today, calendarYearBound);
        List<TimelineCellResponse> cells = buildWeekBucketCells(
                dateRange.startDate(),
                dateRange.endDate(),
                today,
                startOfWeek
        );

        TimelineSettingsResponse settings = new TimelineSettingsResponse(
                TimelineView.QUADRIMESTER,
                startOfWeek,
                scaleNumbering,
                calendarYearBound
        );

        return new TimelineDashboardResponse(
                TimelineView.QUADRIMESTER,
                dateRange.startDate().getYear(),
                dateRange.startDate().getMonthValue(),
                dateRange.startDate(),
                dateRange.endDate(),
                dateRange.label(),
                today,
                false,
                settings,
                buildTimelineScaleBar(dateRange.startDate(), dateRange.endDate(), cells, today),
                cells,
                buildTimelineCategoryResponses(dateRange.startDate(), dateRange.endDate(), cells)
        );
    }

    private TimelineDashboardResponse getHalfYearTimelineDashboard(
            LocalDate today,
            StartOfWeek startOfWeek,
            ScaleNumbering scaleNumbering,
            boolean calendarYearBound
    ) {
        TimelineDateRange dateRange = buildHalfYearDateRange(today, calendarYearBound);
        List<TimelineCellResponse> cells = buildWeekCells(
                dateRange.startDate(),
                dateRange.endDate(),
                today,
                startOfWeek
        );

        TimelineSettingsResponse settings = new TimelineSettingsResponse(
                TimelineView.HALF_YEAR,
                startOfWeek,
                scaleNumbering,
                calendarYearBound
        );

        return new TimelineDashboardResponse(
                TimelineView.HALF_YEAR,
                dateRange.startDate().getYear(),
                dateRange.startDate().getMonthValue(),
                dateRange.startDate(),
                dateRange.endDate(),
                dateRange.label(),
                today,
                false,
                settings,
                buildTimelineScaleBar(dateRange.startDate(), dateRange.endDate(), cells, today),
                cells,
                buildTimelineCategoryResponses(dateRange.startDate(), dateRange.endDate(), cells)
        );
    }

    private TimelineDashboardResponse getYearTimelineDashboard(
            LocalDate today,
            StartOfWeek startOfWeek,
            ScaleNumbering scaleNumbering,
            boolean calendarYearBound
    ) {
        TimelineDateRange dateRange = buildYearDateRange(today, calendarYearBound);
        List<TimelineCellResponse> cells = buildWeekCells(
                dateRange.startDate(),
                dateRange.endDate(),
                today,
                startOfWeek
        );

        TimelineSettingsResponse settings = new TimelineSettingsResponse(
                TimelineView.YEAR,
                startOfWeek,
                scaleNumbering,
                calendarYearBound
        );

        return new TimelineDashboardResponse(
                TimelineView.YEAR,
                dateRange.startDate().getYear(),
                dateRange.startDate().getMonthValue(),
                dateRange.startDate(),
                dateRange.endDate(),
                dateRange.label(),
                today,
                false,
                settings,
                buildTimelineScaleBar(dateRange.startDate(), dateRange.endDate(), cells, today),
                cells,
                buildTimelineCategoryResponses(dateRange.startDate(), dateRange.endDate(), cells)
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

    private List<TimelineCategoryResponse> buildTimelineCategoryResponses(
            LocalDate startDate,
            LocalDate endDate,
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
                List<OccurrenceResponse> occurrences = recurrenceService.generateOccurrencesBetween(
                        task,
                        startDate,
                        endDate
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
                    .filter(occurrence -> occurrenceBelongsInCell(occurrence.occurrenceDate(), cell))
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

    private boolean occurrenceBelongsInCell(LocalDate occurrenceDate, TimelineCellResponse cell) {
        if (occurrenceDate.isBefore(cell.startDate()) || occurrenceDate.isAfter(cell.endDate())) {
            return false;
        }

        if (cell.cellType() == TimelineCellType.WEEKDAY_BUCKET) {
            return !isWeekend(occurrenceDate.getDayOfWeek());
        }

        if (cell.cellType() == TimelineCellType.WEEKEND_BUCKET) {
            return isWeekend(occurrenceDate.getDayOfWeek());
        }

        return true;
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

    private TimelineScaleBarResponse buildTimelineScaleBar(
            LocalDate startDate,
            LocalDate endDate,
            List<TimelineCellResponse> cells,
            LocalDate today
    ) {
        Set<String> anchorCellKeys = new LinkedHashSet<>();
        YearMonth cursor = YearMonth.from(startDate);
        YearMonth finalMonth = YearMonth.from(endDate);

        while (!cursor.isAfter(finalMonth)) {
            LocalDate monthStart = cursor.atDay(1).isBefore(startDate)
                    ? startDate
                    : cursor.atDay(1);
            cells.stream()
                    .filter(cell -> occurrenceBelongsInCell(monthStart, cell))
                    .findFirst()
                    .map(TimelineCellResponse::key)
                    .ifPresent(anchorCellKeys::add);
            cursor = cursor.plusMonths(1);
        }

        String currentDateLabel = today.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                + " "
                + today.getDayOfMonth();

        return new TimelineScaleBarResponse(List.copyOf(anchorCellKeys), currentDateLabel);
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

    private List<TimelineCellResponse> buildWeekCells(
            LocalDate startDate,
            LocalDate endDate,
            LocalDate today,
            StartOfWeek startOfWeek
    ) {
        List<TimelineCellResponse> cells = new ArrayList<>();
        LocalDate weekStart = alignToWeekStart(startDate, startOfWeek);
        int weekIndex = 1;

        while (!weekStart.isAfter(endDate)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            LocalDate cellStart = weekStart.isBefore(startDate) ? startDate : weekStart;
            LocalDate cellEnd = weekEnd.isAfter(endDate) ? endDate : weekEnd;

            cells.add(new TimelineCellResponse(
                    bucketCellKey(weekStart, TimelineCellType.WEEK),
                    cellStart,
                    cellEnd,
                    "W" + weekIndex,
                    cellStart + " to " + cellEnd,
                    TimelineCellType.WEEK,
                    weekIndex,
                    weekIndex,
                    containsToday(cellStart, cellEnd, today, TimelineCellType.WEEK),
                    containsWeekend(cellStart, cellEnd),
                    true
            ));

            weekStart = weekStart.plusWeeks(1);
            weekIndex++;
        }

        return cells;
    }

    private List<TimelineCellResponse> buildWeekBucketCells(
            LocalDate startDate,
            LocalDate endDate,
            LocalDate today,
            StartOfWeek startOfWeek
    ) {
        List<TimelineCellResponse> cells = new ArrayList<>();
        LocalDate weekStart = alignToWeekStart(startDate, startOfWeek);
        int weekIndex = 1;
        int continuedIndex = 1;

        while (!weekStart.isAfter(endDate)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            LocalDate cellStart = weekStart.isBefore(startDate) ? startDate : weekStart;
            LocalDate cellEnd = weekEnd.isAfter(endDate) ? endDate : weekEnd;

            cells.add(new TimelineCellResponse(
                    bucketCellKey(weekStart, TimelineCellType.WEEKDAY_BUCKET),
                    cellStart,
                    cellEnd,
                    "W" + weekIndex,
                    "Weekdays",
                    TimelineCellType.WEEKDAY_BUCKET,
                    weekIndex,
                    continuedIndex++,
                    containsToday(cellStart, cellEnd, today, TimelineCellType.WEEKDAY_BUCKET),
                    false,
                    true
            ));

            cells.add(new TimelineCellResponse(
                    bucketCellKey(weekStart, TimelineCellType.WEEKEND_BUCKET),
                    cellStart,
                    cellEnd,
                    "W" + weekIndex,
                    "Weekend",
                    TimelineCellType.WEEKEND_BUCKET,
                    weekIndex,
                    continuedIndex++,
                    containsToday(cellStart, cellEnd, today, TimelineCellType.WEEKEND_BUCKET),
                    true,
                    true
            ));

            weekStart = weekStart.plusWeeks(1);
            weekIndex++;
        }

        return cells;
    }

    private LocalDate alignToWeekStart(LocalDate date, StartOfWeek startOfWeek) {
        DayOfWeek targetDayOfWeek = startOfWeek == StartOfWeek.SUNDAY
                ? DayOfWeek.SUNDAY
                : DayOfWeek.MONDAY;
        LocalDate cursor = date;

        while (cursor.getDayOfWeek() != targetDayOfWeek) {
            cursor = cursor.minusDays(1);
        }

        return cursor;
    }

    private boolean containsToday(
            LocalDate startDate,
            LocalDate endDate,
            LocalDate today,
            TimelineCellType cellType
    ) {
        return occurrenceBelongsInCell(today, new TimelineCellResponse(
                "",
                startDate,
                endDate,
                "",
                "",
                cellType,
                0,
                0,
                false,
                false,
                true
        ));
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

    private boolean containsWeekend(LocalDate startDate, LocalDate endDate) {
        LocalDate cursor = startDate;

        while (!cursor.isAfter(endDate)) {
            if (isWeekend(cursor.getDayOfWeek())) {
                return true;
            }

            cursor = cursor.plusDays(1);
        }

        return false;
    }

    private String buildMonthLabel(YearMonth targetMonth) {
        return targetMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " "
                + targetMonth.getYear();
    }

    private TimelineDateRange buildQuarterDateRange(LocalDate today, boolean calendarYearBound) {
        YearMonth currentMonth = YearMonth.from(today);

        if (!calendarYearBound) {
            YearMonth endMonth = currentMonth.plusMonths(2);
            return new TimelineDateRange(
                    currentMonth.atDay(1),
                    endMonth.atEndOfMonth(),
                    buildRangeLabel(currentMonth, endMonth)
            );
        }

        int quarterStartMonth = ((today.getMonthValue() - 1) / 3) * 3 + 1;
        YearMonth startMonth = YearMonth.of(today.getYear(), quarterStartMonth);
        YearMonth endMonth = startMonth.plusMonths(2);

        return new TimelineDateRange(
                startMonth.atDay(1),
                endMonth.atEndOfMonth(),
                "Q" + (((quarterStartMonth - 1) / 3) + 1) + " " + today.getYear()
        );
    }

    private TimelineDateRange buildQuadrimesterDateRange(LocalDate today, boolean calendarYearBound) {
        YearMonth currentMonth = YearMonth.from(today);

        if (!calendarYearBound) {
            YearMonth endMonth = currentMonth.plusMonths(3);
            return new TimelineDateRange(
                    currentMonth.atDay(1),
                    endMonth.atEndOfMonth(),
                    buildRangeLabel(currentMonth, endMonth)
            );
        }

        int quadrimesterStartMonth = ((today.getMonthValue() - 1) / 4) * 4 + 1;
        YearMonth startMonth = YearMonth.of(today.getYear(), quadrimesterStartMonth);
        YearMonth endMonth = startMonth.plusMonths(3);

        return new TimelineDateRange(
                startMonth.atDay(1),
                endMonth.atEndOfMonth(),
                "Quadrimester " + (((quadrimesterStartMonth - 1) / 4) + 1) + " " + today.getYear()
        );
    }

    private TimelineDateRange buildHalfYearDateRange(LocalDate today, boolean calendarYearBound) {
        YearMonth currentMonth = YearMonth.from(today);

        if (!calendarYearBound) {
            YearMonth endMonth = currentMonth.plusMonths(5);
            return new TimelineDateRange(
                    currentMonth.atDay(1),
                    endMonth.atEndOfMonth(),
                    buildRangeLabel(currentMonth, endMonth)
            );
        }

        int halfYearStartMonth = today.getMonthValue() <= 6 ? 1 : 7;
        YearMonth startMonth = YearMonth.of(today.getYear(), halfYearStartMonth);
        YearMonth endMonth = startMonth.plusMonths(5);

        return new TimelineDateRange(
                startMonth.atDay(1),
                endMonth.atEndOfMonth(),
                "H" + (halfYearStartMonth == 1 ? "1" : "2") + " " + today.getYear()
        );
    }

    private TimelineDateRange buildYearDateRange(LocalDate today, boolean calendarYearBound) {
        YearMonth currentMonth = YearMonth.from(today);

        if (!calendarYearBound) {
            YearMonth endMonth = currentMonth.plusMonths(11);
            return new TimelineDateRange(
                    currentMonth.atDay(1),
                    endMonth.atEndOfMonth(),
                    buildRangeLabel(currentMonth, endMonth)
            );
        }

        YearMonth startMonth = YearMonth.of(today.getYear(), 1);
        YearMonth endMonth = YearMonth.of(today.getYear(), 12);

        return new TimelineDateRange(
                startMonth.atDay(1),
                endMonth.atEndOfMonth(),
                String.valueOf(today.getYear())
        );
    }

    private String buildRangeLabel(YearMonth startMonth, YearMonth endMonth) {
        String startLabel = startMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        String endLabel = endMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

        if (startMonth.getYear() == endMonth.getYear()) {
            return startLabel + "-" + endLabel + " " + startMonth.getYear();
        }

        return startLabel + " " + startMonth.getYear() + "-" + endLabel + " " + endMonth.getYear();
    }

    private String cellKey(LocalDate date) {
        return date.toString();
    }

    private String bucketCellKey(LocalDate weekStart, TimelineCellType cellType) {
        return weekStart + "-" + cellType;
    }

    private record TimelineDateRange(
            LocalDate startDate,
            LocalDate endDate,
            String label
    ) {
    }
}
