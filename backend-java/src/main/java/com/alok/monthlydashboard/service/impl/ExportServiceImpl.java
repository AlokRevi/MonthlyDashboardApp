package com.alok.monthlydashboard.service.impl;

import com.alok.monthlydashboard.dto.export.ExportCategoryResponse;
import com.alok.monthlydashboard.dto.export.ExportCompletionResponse;
import com.alok.monthlydashboard.dto.export.ExportFixedDateResponse;
import com.alok.monthlydashboard.dto.export.ExportRecurrenceRuleResponse;
import com.alok.monthlydashboard.dto.export.ExportTaskResponse;
import com.alok.monthlydashboard.dto.export.SetupSnapshotCategoryResponse;
import com.alok.monthlydashboard.dto.export.SetupSnapshotResponse;
import com.alok.monthlydashboard.dto.export.SetupSnapshotRuleResponse;
import com.alok.monthlydashboard.dto.export.SetupSnapshotTaskResponse;
import com.alok.monthlydashboard.dto.export.SystemExportResponse;
import com.alok.monthlydashboard.entity.Category;
import com.alok.monthlydashboard.entity.Task;
import com.alok.monthlydashboard.entity.TaskCompletion;
import com.alok.monthlydashboard.entity.TaskFixedDate;
import com.alok.monthlydashboard.entity.TaskRecurrenceRule;
import com.alok.monthlydashboard.repository.CategoryRepository;
import com.alok.monthlydashboard.repository.TaskCompletionRepository;
import com.alok.monthlydashboard.repository.TaskRepository;
import com.alok.monthlydashboard.service.ExportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ExportServiceImpl implements ExportService {

    private final CategoryRepository categoryRepository;
    private final TaskRepository taskRepository;
    private final TaskCompletionRepository taskCompletionRepository;

    public ExportServiceImpl(
            CategoryRepository categoryRepository,
            TaskRepository taskRepository,
            TaskCompletionRepository taskCompletionRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.taskRepository = taskRepository;
        this.taskCompletionRepository = taskCompletionRepository;
    }

    @Override
    public SystemExportResponse exportSystemState() {
        List<Category> categories = categoryRepository.findAll();
        List<Task> tasks = taskRepository.findAll();
        List<TaskCompletion> completions = taskCompletionRepository.findAll();

        return new SystemExportResponse(
                categories.stream()
                        .sorted(Comparator.comparing(Category::getId))
                        .map(this::toCategoryResponse)
                        .toList(),
                tasks.stream()
                        .sorted(Comparator.comparing(Task::getId))
                        .map(this::toTaskResponse)
                        .toList(),
                tasks.stream()
                        .sorted(Comparator.comparing(Task::getId))
                        .map(this::toRecurrenceRuleResponse)
                        .toList(),
                completions.stream()
                        .sorted(Comparator.comparing(TaskCompletion::getOccurrenceDate)
                                .thenComparing(completion -> completion.getTask().getId()))
                        .map(this::toCompletionResponse)
                        .toList()
        );
    }

    @Override
    public SetupSnapshotResponse exportSetupSnapshot() {
        List<Category> categories = categoryRepository.findAll();
        List<Task> tasks = taskRepository.findAll();

        return new SetupSnapshotResponse(
                LocalDateTime.now(),
                "v2-setup-snapshot",
                categories.stream()
                        .sorted(Comparator.comparing(Category::getId))
                        .map(this::toSetupCategoryResponse)
                        .toList(),
                tasks.stream()
                        .sorted(Comparator.comparing(Task::getId))
                        .map(this::toSetupTaskResponse)
                        .toList()
        );
    }

    private ExportCategoryResponse toCategoryResponse(Category category) {
        return new ExportCategoryResponse(
                category.getId(),
                category.getName(),
                category.getColor(),
                category.getRequires(),
                List.copyOf(category.getFeelsLike()),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private ExportTaskResponse toTaskResponse(Task task) {
        return new ExportTaskResponse(
                task.getId(),
                task.getCategory().getId(),
                task.getName(),
                task.getDescription(),
                task.getRecurrenceType(),
                task.getStartDate(),
                task.getEndDate(),
                task.isActive(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private ExportRecurrenceRuleResponse toRecurrenceRuleResponse(Task task) {
        TaskRecurrenceRule rule = task.getRecurrenceRule();

        return new ExportRecurrenceRuleResponse(
                rule == null ? null : rule.getId(),
                task.getId(),
                rule == null ? null : rule.getIntervalValue(),
                rule == null ? null : rule.getIntervalUnit(),
                rule == null ? null : rule.getWeekday(),
                rule == null ? null : rule.getWeekOfMonth(),
                rule == null ? null : rule.isFallbackToLastDay(),
                task.getFixedDates().stream()
                        .sorted(Comparator.comparing(TaskFixedDate::getDayOfMonth))
                        .map(this::toFixedDateResponse)
                        .toList(),
                rule == null ? null : rule.getCreatedAt(),
                rule == null ? null : rule.getUpdatedAt()
        );
    }

    private ExportFixedDateResponse toFixedDateResponse(TaskFixedDate fixedDate) {
        return new ExportFixedDateResponse(
                fixedDate.getId(),
                fixedDate.getDayOfMonth(),
                fixedDate.getCreatedAt()
        );
    }

    private ExportCompletionResponse toCompletionResponse(TaskCompletion completion) {
        return new ExportCompletionResponse(
                completion.getId(),
                completion.getTask().getId(),
                completion.getOccurrenceDate(),
                completion.getCompletionDate(),
                completion.getCreatedAt(),
                completion.getUpdatedAt()
        );
    }

    private SetupSnapshotCategoryResponse toSetupCategoryResponse(Category category) {
        return new SetupSnapshotCategoryResponse(
                category.getId(),
                category.getName(),
                category.getColor(),
                category.getRequires(),
                List.copyOf(category.getFeelsLike()),
                category.getTasks().size()
        );
    }

    private SetupSnapshotTaskResponse toSetupTaskResponse(Task task) {
        return new SetupSnapshotTaskResponse(
                task.getId(),
                task.getCategory().getId(),
                task.getCategory().getName(),
                task.getName(),
                task.getDescription(),
                task.getRecurrenceType(),
                task.getStartDate(),
                task.getEndDate(),
                task.isActive(),
                task.getEnergyOverride(),
                task.getEnjoymentOverride(),
                task.getPressureOverride(),
                task.getEffortOverride(),
                toSetupRuleResponse(task)
        );
    }

    private SetupSnapshotRuleResponse toSetupRuleResponse(Task task) {
        TaskRecurrenceRule rule = task.getRecurrenceRule();

        return new SetupSnapshotRuleResponse(
                task.getFixedDates().stream()
                        .sorted(Comparator.comparing(TaskFixedDate::getDayOfMonth))
                        .map(TaskFixedDate::getDayOfMonth)
                        .toList(),
                rule == null || rule.isFallbackToLastDay(),
                rule == null ? null : rule.getIntervalValue(),
                rule == null ? null : rule.getIntervalUnit(),
                rule == null ? null : rule.getWeekday(),
                rule == null ? null : rule.getWeekOfMonth()
        );
    }
}
