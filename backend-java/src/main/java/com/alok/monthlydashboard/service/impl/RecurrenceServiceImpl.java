package com.alok.monthlydashboard.service.impl;

import com.alok.monthlydashboard.dto.dashboard.OccurrenceResponse;
import com.alok.monthlydashboard.entity.Task;
import com.alok.monthlydashboard.entity.TaskCompletion;
import com.alok.monthlydashboard.entity.TaskFixedDate;
import com.alok.monthlydashboard.entity.TaskRecurrenceRule;
import com.alok.monthlydashboard.entity.enums.IntervalUnit;
import com.alok.monthlydashboard.entity.enums.WeekOfMonth;
import com.alok.monthlydashboard.entity.enums.Weekday;
import com.alok.monthlydashboard.exception.ResourceNotFoundException;
import com.alok.monthlydashboard.repository.TaskCompletionRepository;
import com.alok.monthlydashboard.repository.TaskRepository;
import com.alok.monthlydashboard.service.AppDateProvider;
import com.alok.monthlydashboard.service.RecurrenceService;
import org.springframework.stereotype.Service;
import com.alok.monthlydashboard.common.enums.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class RecurrenceServiceImpl implements RecurrenceService {

    private final TaskRepository taskRepository;
    private final TaskCompletionRepository taskCompletionRepository;
    private final AppDateProvider appDateProvider;

    public RecurrenceServiceImpl(
            TaskRepository taskRepository,
            TaskCompletionRepository taskCompletionRepository,
            AppDateProvider appDateProvider
    ) {
        this.taskRepository = taskRepository;
        this.taskCompletionRepository = taskCompletionRepository;
        this.appDateProvider = appDateProvider;
    }

    @Override
    public List<OccurrenceResponse> generateOccurrencesForMonth(Long taskId, int year, int month) {
        Task task = getTaskOrThrow(taskId);
        YearMonth targetMonth = YearMonth.of(year, month);
        return generateOccurrencesBetween(
                task,
                targetMonth.atDay(1),
                targetMonth.atEndOfMonth()
        );
    }

    @Override
    public List<OccurrenceResponse> generateOccurrencesBetween(
            Long taskId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return generateOccurrencesBetween(getTaskOrThrow(taskId), startDate, endDate);
    }

    @Override
    public List<OccurrenceResponse> generateOccurrencesBetween(
            Task task,
            LocalDate startDate,
            LocalDate endDate
    ) {
        LocalDate today = appDateProvider.today();
        List<LocalDate> occurrenceDates = generateOccurrenceDatesBetween(task, startDate, endDate);
        List<OccurrenceResponse> responses = new ArrayList<>();

        for (LocalDate occurrenceDate : occurrenceDates) {
            Optional<TaskCompletion> completionOpt =
                    taskCompletionRepository.findByTaskIdAndOccurrenceDate(task.getId(), occurrenceDate);

            boolean completed = completionOpt.isPresent();
            LocalDate completionDate = completionOpt.map(TaskCompletion::getCompletionDate).orElse(null);

            OccurrenceStatus status = determineStatus(occurrenceDate, completed, today);

            responses.add(new OccurrenceResponse(
                    occurrenceDate,
                    occurrenceDate.getDayOfMonth(),
                    completed,
                    completionDate,
                    status
            ));
        }

        responses.sort(Comparator.comparing(OccurrenceResponse::occurrenceDate));
        return responses;
    }

    @Override
    public List<LocalDate> generateOccurrenceDatesForMonth(Long taskId, int year, int month) {
        Task task = getTaskOrThrow(taskId);
        YearMonth targetMonth = YearMonth.of(year, month);
        return generateOccurrenceDatesBetween(
                task,
                targetMonth.atDay(1),
                targetMonth.atEndOfMonth()
        );
    }

    @Override
    public List<LocalDate> generateOccurrenceDatesBetween(
            Long taskId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return generateOccurrenceDatesBetween(getTaskOrThrow(taskId), startDate, endDate);
    }

    @Override
    public List<LocalDate> generateOccurrenceDatesBetween(
            Task task,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate.isAfter(endDate)) {
            return List.of();
        }

        List<LocalDate> dates = new ArrayList<>();
        YearMonth cursor = YearMonth.from(startDate);
        YearMonth finalMonth = YearMonth.from(endDate);

        while (!cursor.isAfter(finalMonth)) {
            dates.addAll(generateOccurrenceDates(task, cursor)
                    .stream()
                    .filter(date -> !date.isBefore(startDate) && !date.isAfter(endDate))
                    .toList());
            cursor = cursor.plusMonths(1);
        }

        dates.sort(LocalDate::compareTo);
        return dates;
    }

    @Override
    public boolean isValidOccurrence(Long taskId, LocalDate occurrenceDate) {
        Task task = getTaskOrThrow(taskId);

        // quick lifetime check
        if (occurrenceDate.isBefore(task.getStartDate())) {
            return false;
        }
        if (task.getEndDate() != null && occurrenceDate.isAfter(task.getEndDate())) {
            return false;
        }

        List<LocalDate> datesForMonth =
                generateOccurrenceDates(task, YearMonth.from(occurrenceDate));

        return datesForMonth.contains(occurrenceDate);
    }

    private Task getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
    }

    private List<LocalDate> generateOccurrenceDates(Task task, YearMonth targetMonth) {
        if (!task.isActive()) {
            return List.of();
        }

        return switch (task.getRecurrenceType()) {
            case FIXED_DATE -> generateFixedDateOccurrences(task, targetMonth);
            case INTERVAL -> generateIntervalOccurrences(task, targetMonth);
            case WEEKDAY -> generateWeekdayOccurrences(task, targetMonth);
        };
    }

    private List<LocalDate> generateFixedDateOccurrences(Task task, YearMonth targetMonth) {
        List<LocalDate> dates = new ArrayList<>();

        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();

        // if month is entirely outside task lifetime, return empty
        if (monthEnd.isBefore(task.getStartDate())) {
            return dates;
        }
        if (task.getEndDate() != null && monthStart.isAfter(task.getEndDate())) {
            return dates;
        }

        boolean fallbackToLastDay = fallbackToLastDay(task);

        for (TaskFixedDate fixedDate : task.getFixedDates()) {
            int requestedDay = fixedDate.getDayOfMonth();
            int actualDay = requestedDay;

            if (requestedDay > targetMonth.lengthOfMonth()) {
                if (fallbackToLastDay) {
                    actualDay = targetMonth.lengthOfMonth();
                } else {
                    continue;
                }
            }

            LocalDate occurrenceDate = targetMonth.atDay(actualDay);

            if (occurrenceDate.isBefore(task.getStartDate())) {
                continue;
            }
            if (task.getEndDate() != null && occurrenceDate.isAfter(task.getEndDate())) {
                continue;
            }

            if (!dates.contains(occurrenceDate)) {
                dates.add(occurrenceDate);
            }
        }

        dates.sort(LocalDate::compareTo);
        return dates;
    }

    private List<LocalDate> generateIntervalOccurrences(Task task, YearMonth targetMonth) {
        List<LocalDate> dates = new ArrayList<>();
        TaskRecurrenceRule rule = getRuleOrThrow(task);

        if (rule.getIntervalValue() == null || rule.getIntervalValue() <= 0) {
            return dates;
        }
        if (rule.getIntervalUnit() == null) {
            return dates;
        }

        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();

        if (monthEnd.isBefore(task.getStartDate())) {
            return dates;
        }
        if (task.getEndDate() != null && monthStart.isAfter(task.getEndDate())) {
            return dates;
        }

        if (rule.getIntervalUnit() == IntervalUnit.MONTHS) {
            return generateMonthlyIntervalOccurrences(task, targetMonth, rule.getIntervalValue());
        }

        LocalDate cursor = task.getStartDate();

        while (cursor.isBefore(monthStart)) {
            cursor = advanceInterval(cursor, rule.getIntervalValue(), rule.getIntervalUnit());
        }

        while (!cursor.isAfter(monthEnd)) {
            if (!cursor.isBefore(task.getStartDate())
                    && (task.getEndDate() == null || !cursor.isAfter(task.getEndDate()))) {
                dates.add(cursor);
            }
            cursor = advanceInterval(cursor, rule.getIntervalValue(), rule.getIntervalUnit());
        }

        return dates;
    }

    private List<LocalDate> generateMonthlyIntervalOccurrences(Task task, YearMonth targetMonth, int intervalValue) {
        List<LocalDate> dates = new ArrayList<>();

        YearMonth startMonth = YearMonth.from(task.getStartDate());
        long monthsFromStart = ChronoUnit.MONTHS.between(startMonth, targetMonth);

        if (monthsFromStart < 0 || monthsFromStart % intervalValue != 0) {
            return dates;
        }

        LocalDate occurrenceDate = anchoredMonthlyDate(targetMonth, task.getStartDate().getDayOfMonth());

        if (occurrenceDate.isBefore(task.getStartDate())) {
            return dates;
        }
        if (task.getEndDate() != null && occurrenceDate.isAfter(task.getEndDate())) {
            return dates;
        }

        dates.add(occurrenceDate);
        return dates;
    }

    private LocalDate anchoredMonthlyDate(YearMonth targetMonth, int anchorDay) {
        return targetMonth.atDay(Math.min(anchorDay, targetMonth.lengthOfMonth()));
    }

    private List<LocalDate> generateWeekdayOccurrences(Task task, YearMonth targetMonth) {
        List<LocalDate> dates = new ArrayList<>();
        TaskRecurrenceRule rule = getRuleOrThrow(task);

        if (rule.getWeekday() == null || rule.getWeekOfMonth() == null) {
            return dates;
        }

        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();

        if (monthEnd.isBefore(task.getStartDate())) {
            return dates;
        }
        if (task.getEndDate() != null && monthStart.isAfter(task.getEndDate())) {
            return dates;
        }

        DayOfWeek targetDayOfWeek = mapWeekday(rule.getWeekday());
        List<LocalDate> matchingDays = new ArrayList<>();

        LocalDate cursor = monthStart;
        while (!cursor.isAfter(monthEnd)) {
            if (cursor.getDayOfWeek() == targetDayOfWeek) {
                matchingDays.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }

        LocalDate occurrenceDate = selectWeekdayOccurrence(matchingDays, rule.getWeekOfMonth());

        if (occurrenceDate == null) {
            return dates;
        }

        if (occurrenceDate.isBefore(task.getStartDate())) {
            return dates;
        }
        if (task.getEndDate() != null && occurrenceDate.isAfter(task.getEndDate())) {
            return dates;
        }

        dates.add(occurrenceDate);
        return dates;
    }

    private TaskRecurrenceRule getRuleOrThrow(Task task) {
        TaskRecurrenceRule rule = task.getRecurrenceRule();
        if (rule == null) {
            throw new ResourceNotFoundException(
                    "Recurrence rule not found for task id: " + task.getId()
            );
        }
        return rule;
    }

    private boolean fallbackToLastDay(Task task) {
        TaskRecurrenceRule rule = task.getRecurrenceRule();
        return rule == null || rule.isFallbackToLastDay();
    }

    private LocalDate advanceInterval(LocalDate date, int intervalValue, IntervalUnit unit) {
        return switch (unit) {
            case DAYS -> date.plusDays(intervalValue);
            case WEEKS -> date.plusWeeks(intervalValue);
            case MONTHS -> throw new IllegalArgumentException("Monthly intervals require start-date anchored generation");
        };
    }

    private DayOfWeek mapWeekday(Weekday weekday) {
        return switch (weekday) {
            case MONDAY -> DayOfWeek.MONDAY;
            case TUESDAY -> DayOfWeek.TUESDAY;
            case WEDNESDAY -> DayOfWeek.WEDNESDAY;
            case THURSDAY -> DayOfWeek.THURSDAY;
            case FRIDAY -> DayOfWeek.FRIDAY;
            case SATURDAY -> DayOfWeek.SATURDAY;
            case SUNDAY -> DayOfWeek.SUNDAY;
        };
    }

    private LocalDate selectWeekdayOccurrence(List<LocalDate> matchingDays, WeekOfMonth weekOfMonth) {
        if (matchingDays.isEmpty()) {
            return null;
        }

        return switch (weekOfMonth) {
            case FIRST -> getByIndexOrLast(matchingDays, 0);
            case SECOND -> getByIndexOrLast(matchingDays, 1);
            case THIRD -> getByIndexOrLast(matchingDays, 2);
            case FOURTH -> getByIndexOrLast(matchingDays, 3);
            case LAST -> matchingDays.get(matchingDays.size() - 1);
        };
    }

    /**
     * Your product decision:
     * if nth weekday doesn't exist, treat as last.
     * Example: "5th Monday" style fallback behavior conceptually maps to last available.
     */
    private LocalDate getByIndexOrLast(List<LocalDate> dates, int index) {
        if (index < dates.size()) {
            return dates.get(index);
        }
        return dates.get(dates.size() - 1);
    }

    private OccurrenceStatus determineStatus(LocalDate occurrenceDate, boolean completed, LocalDate today) {
        if (completed) {
            return OccurrenceStatus.COMPLETED;
        }
        if (occurrenceDate.isEqual(today)) {
            return OccurrenceStatus.DUE_TODAY;
        }
        if (occurrenceDate.isBefore(today)) {
            return OccurrenceStatus.OVERDUE;
        }
        return OccurrenceStatus.UPCOMING;
    }
}
