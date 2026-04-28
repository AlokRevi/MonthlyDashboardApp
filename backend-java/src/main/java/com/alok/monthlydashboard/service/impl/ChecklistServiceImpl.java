package com.alok.monthlydashboard.service.impl;

import com.alok.monthlydashboard.common.enums.OccurrenceStatus;
import com.alok.monthlydashboard.dto.checklist.ChecklistItemResponse;
import com.alok.monthlydashboard.dto.checklist.CompleteTaskRequest;
import com.alok.monthlydashboard.dto.checklist.CompletionResponse;
import com.alok.monthlydashboard.dto.checklist.TodayChecklistResponse;
import com.alok.monthlydashboard.entity.Task;
import com.alok.monthlydashboard.entity.TaskCompletion;
import com.alok.monthlydashboard.exception.ConflictException;
import com.alok.monthlydashboard.exception.ResourceNotFoundException;
import com.alok.monthlydashboard.exception.ValidationException;
import com.alok.monthlydashboard.repository.TaskCompletionRepository;
import com.alok.monthlydashboard.repository.TaskRepository;
import com.alok.monthlydashboard.service.AppDateProvider;
import com.alok.monthlydashboard.service.ChecklistService;
import com.alok.monthlydashboard.service.RecurrenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChecklistServiceImpl implements ChecklistService {

    private final TaskRepository taskRepository;
    private final TaskCompletionRepository taskCompletionRepository;
    private final RecurrenceService recurrenceService;
    private final AppDateProvider appDateProvider;

    public ChecklistServiceImpl(
            TaskRepository taskRepository,
            TaskCompletionRepository taskCompletionRepository,
            RecurrenceService recurrenceService,
            AppDateProvider appDateProvider
    ) {
        this.taskRepository = taskRepository;
        this.taskCompletionRepository = taskCompletionRepository;
        this.recurrenceService = recurrenceService;
        this.appDateProvider = appDateProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public TodayChecklistResponse getTodayChecklist() {
        LocalDate today = appDateProvider.today();

        List<Task> activeTasks = taskRepository.findByIsActiveOrderByNameAsc(true);
        List<ChecklistItemResponse> items = new ArrayList<>();

        for (Task task : activeTasks) {
            List<LocalDate> candidateDates = collectCandidateDates(task, today);

            for (LocalDate occurrenceDate : candidateDates) {
                Optional<TaskCompletion> completion = taskCompletionRepository
                        .findByTaskIdAndOccurrenceDate(task.getId(), occurrenceDate);

                if (completion.isPresent()) {
                    if (today.equals(completion.get().getCompletionDate())) {
                        items.add(new ChecklistItemResponse(
                                task.getId(),
                                task.getName(),
                                task.getCategory().getId(),
                                task.getCategory().getName(),
                                occurrenceDate,
                                OccurrenceStatus.COMPLETED
                        ));
                    }
                    continue;
                }

                OccurrenceStatus status = occurrenceDate.isEqual(today)
                        ? OccurrenceStatus.DUE_TODAY
                        : OccurrenceStatus.OVERDUE;

                items.add(new ChecklistItemResponse(
                        task.getId(),
                        task.getName(),
                        task.getCategory().getId(),
                        task.getCategory().getName(),
                        occurrenceDate,
                        status
                ));
            }
        }

        items.sort(
                Comparator.comparing(ChecklistItemResponse::occurrenceDate)
                        .thenComparing(ChecklistItemResponse::taskName)
        );

        return new TodayChecklistResponse(today, items);
    }

    @Override
    public CompletionResponse completeTask(Long taskId, CompleteTaskRequest request) {
        Task task = getTaskOrThrow(taskId);

        LocalDate today = appDateProvider.today();
        LocalDate occurrenceDate = request.occurrenceDate();
        LocalDate completionDate = request.completionDate();

        if (!task.isActive()) {
            throw new ValidationException("Inactive task cannot be completed");
        }

        if (occurrenceDate.isAfter(today)) {
            throw new ValidationException("Future occurrence cannot be completed");
        }

        if (completionDate.isAfter(today)) {
            throw new ValidationException("Future completion is not allowed");
        }

        if (completionDate.isBefore(occurrenceDate)) {
            throw new ValidationException("Completion date cannot be before occurrence date");
        }

        if (!recurrenceService.isValidOccurrence(taskId, occurrenceDate)) {
            throw new ValidationException("Provided occurrence date is not valid for this task");
        }

        boolean alreadyCompleted = taskCompletionRepository
                .existsByTaskIdAndOccurrenceDate(taskId, occurrenceDate);

        if (alreadyCompleted) {
            throw new ConflictException("This task occurrence is already completed");
        }

        TaskCompletion completion = new TaskCompletion();
        completion.setTask(task);
        completion.setOccurrenceDate(occurrenceDate);
        completion.setCompletionDate(completionDate);

        taskCompletionRepository.save(completion);

        return new CompletionResponse(
                taskId,
                occurrenceDate,
                completionDate,
                OccurrenceStatus.COMPLETED,
                "Task marked complete"
        );
    }

    @Override
    public void undoCompletion(Long taskId, LocalDate occurrenceDate) {
        getTaskOrThrow(taskId);

        TaskCompletion completion = taskCompletionRepository
                .findByTaskIdAndOccurrenceDate(taskId, occurrenceDate)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Completion not found for task id " + taskId + " and occurrence date " + occurrenceDate
                ));

        taskCompletionRepository.delete(completion);
    }

    private Task getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
    }

    private List<LocalDate> collectCandidateDates(Task task, LocalDate today) {
        List<LocalDate> allDates = new ArrayList<>();

        YearMonth currentMonth = YearMonth.from(today);
        YearMonth previousMonth = currentMonth.minusMonths(1);

        allDates.addAll(recurrenceService.generateOccurrenceDatesForMonth(
                task.getId(),
                previousMonth.getYear(),
                previousMonth.getMonthValue()
        ));

        allDates.addAll(recurrenceService.generateOccurrenceDatesForMonth(
                task.getId(),
                currentMonth.getYear(),
                currentMonth.getMonthValue()
        ));

        return allDates.stream()
                .filter(date -> !date.isAfter(today))
                .sorted()
                .toList();
    }
}
