package com.alok.monthlydashboard.service.impl;

import com.alok.monthlydashboard.common.enums.RecurrenceType;
import com.alok.monthlydashboard.dto.task.CreateTaskRequest;
import com.alok.monthlydashboard.dto.task.TaskDetailResponse;
import com.alok.monthlydashboard.dto.task.TaskMutationResponse;
import com.alok.monthlydashboard.dto.task.TaskRuleRequest;
import com.alok.monthlydashboard.dto.task.TaskSummaryResponse;
import com.alok.monthlydashboard.dto.task.UpdateTaskRequest;
import com.alok.monthlydashboard.entity.Category;
import com.alok.monthlydashboard.entity.Task;
import com.alok.monthlydashboard.entity.TaskFixedDate;
import com.alok.monthlydashboard.entity.TaskRecurrenceRule;
import com.alok.monthlydashboard.exception.ConflictException;
import com.alok.monthlydashboard.exception.ResourceNotFoundException;
import com.alok.monthlydashboard.exception.ValidationException;
import com.alok.monthlydashboard.repository.CategoryRepository;
import com.alok.monthlydashboard.repository.TaskCompletionRepository;
import com.alok.monthlydashboard.repository.TaskRepository;
import com.alok.monthlydashboard.service.TaskService;
import com.alok.monthlydashboard.util.TaskMapper;
import com.alok.monthlydashboard.util.TaskValidationHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private static final int MAX_ACTIVE_TASKS = 15;

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final TaskCompletionRepository taskCompletionRepository;

    public TaskServiceImpl(
            TaskRepository taskRepository,
            CategoryRepository categoryRepository,
            TaskCompletionRepository taskCompletionRepository
    ) {
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
        this.taskCompletionRepository = taskCompletionRepository;
    }

    @Override
    public TaskDetailResponse createTask(CreateTaskRequest request) {
        validateDates(request.startDate(), request.endDate());
        TaskValidationHelper.validateRule(request.recurrenceType(), request.rule());

        Category category = getCategoryOrThrow(request.categoryId());

        long activeCount = taskRepository.countByIsActive(true);
        if (activeCount >= MAX_ACTIVE_TASKS) {
            throw new ValidationException("Maximum of 15 active tasks allowed");
        }

        Task task = new Task();
        task.setCategory(category);
        task.setName(request.name());
        task.setDescription(request.description());
        task.setRecurrenceType(request.recurrenceType());
        task.setStartDate(request.startDate());
        task.setEndDate(request.endDate());
        task.setActive(true);

        TaskRecurrenceRule rule = buildRuleEntity(request.rule());
        task.setRecurrenceRule(rule);

        if (request.recurrenceType() == RecurrenceType.FIXED_DATE) {
            for (Integer day : request.rule().fixedDates()) {
                TaskFixedDate fixedDate = new TaskFixedDate();
                fixedDate.setDayOfMonth(day);
                task.addFixedDate(fixedDate);
            }
        }

        Task saved = taskRepository.save(task);
        return TaskMapper.toTaskDetailResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskSummaryResponse> getTasks(Long categoryId, Boolean active) {
        List<Task> tasks;

        if (categoryId != null && active != null) {
            ensureCategoryExists(categoryId);
            tasks = taskRepository.findByCategoryIdAndIsActiveOrderByNameAsc(categoryId, active);
        } else if (categoryId != null) {
            ensureCategoryExists(categoryId);
            tasks = taskRepository.findByCategoryIdOrderByNameAsc(categoryId);
        } else if (active != null) {
            tasks = taskRepository.findByIsActiveOrderByNameAsc(active);
        } else {
            tasks = taskRepository.findAllByOrderByNameAsc();
        }

        return tasks.stream()
                .map(TaskMapper::toTaskSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDetailResponse getTask(Long taskId) {
        Task task = getTaskOrThrow(taskId);
        return TaskMapper.toTaskDetailResponse(task);
    }

    @Override
    public TaskMutationResponse updateTask(Long taskId, UpdateTaskRequest request) {
        validateDates(request.startDate(), request.endDate());
        TaskValidationHelper.validateRule(request.recurrenceType(), request.rule());

        Task task = getTaskOrThrow(taskId);
        Category category = getCategoryOrThrow(request.categoryId());

        boolean activatingInactiveTask = !task.isActive() && request.isActive();
        if (activatingInactiveTask) {
            long activeCount = taskRepository.countByIsActive(true);
            if (activeCount >= MAX_ACTIVE_TASKS) {
                throw new ValidationException("Maximum of 15 active tasks allowed");
            }
        }

        task.setCategory(category);
        task.setName(request.name());
        task.setDescription(request.description());
        task.setRecurrenceType(request.recurrenceType());
        task.setStartDate(request.startDate());
        task.setEndDate(request.endDate());
        task.setActive(request.isActive());

        TaskRecurrenceRule rule = task.getRecurrenceRule();
        if (rule == null) {
            rule = new TaskRecurrenceRule();
            task.setRecurrenceRule(rule);
        }
        applyRuleValues(rule, request.rule());

        task.clearFixedDates();
        if (request.recurrenceType() == RecurrenceType.FIXED_DATE) {
            for (Integer day : request.rule().fixedDates()) {
                TaskFixedDate fixedDate = new TaskFixedDate();
                fixedDate.setDayOfMonth(day);
                task.addFixedDate(fixedDate);
            }
        }

        Task saved = taskRepository.save(task);

        return new TaskMutationResponse(
                saved.getId(),
                saved.isActive(),
                "Task updated successfully",
                LocalDateTime.now()
        );
    }

    @Override
    public TaskMutationResponse activateTask(Long taskId) {
        Task task = getTaskOrThrow(taskId);

        if (task.isActive()) {
            return new TaskMutationResponse(task.getId(), true, "Task already active", LocalDateTime.now());
        }

        long activeCount = taskRepository.countByIsActive(true);
        if (activeCount >= MAX_ACTIVE_TASKS) {
            throw new ConflictException("Cannot activate task. Maximum of 15 active tasks allowed");
        }

        task.setActive(true);
        taskRepository.save(task);

        return new TaskMutationResponse(task.getId(), true, "Task activated successfully", LocalDateTime.now());
    }

    @Override
    public TaskMutationResponse deactivateTask(Long taskId) {
        Task task = getTaskOrThrow(taskId);

        if (!task.isActive()) {
            return new TaskMutationResponse(task.getId(), false, "Task already inactive", LocalDateTime.now());
        }

        task.setActive(false);
        taskRepository.save(task);

        return new TaskMutationResponse(task.getId(), false, "Task deactivated successfully", LocalDateTime.now());
    }

    @Override
    public TaskMutationResponse deleteTask(Long taskId, boolean deleteHistory, boolean confirm) {
        Task task = getTaskOrThrow(taskId);

        if (deleteHistory && !confirm) {
            throw new ValidationException("Explicit confirmation is required to delete task history");
        }

        if (!deleteHistory) {
            task.setActive(false);
            taskRepository.save(task);
            return new TaskMutationResponse(
                    task.getId(),
                    false,
                    "Task deleted successfully. History preserved.",
                    LocalDateTime.now()
            );
        }

        taskCompletionRepository.deleteAll(task.getCompletions());
        taskRepository.delete(task);

        return new TaskMutationResponse(
                taskId,
                false,
                "Task and history deleted successfully",
                LocalDateTime.now()
        );
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new ValidationException("endDate must be on or after startDate");
        }
    }

    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }

    private void ensureCategoryExists(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }
    }

    private Task getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
    }

    private TaskRecurrenceRule buildRuleEntity(TaskRuleRequest request) {
        TaskRecurrenceRule rule = new TaskRecurrenceRule();
        applyRuleValues(rule, request);
        return rule;
    }

    private void applyRuleValues(TaskRecurrenceRule rule, TaskRuleRequest request) {
        rule.setIntervalValue(request.intervalValue());
        rule.setIntervalUnit(request.intervalUnit() == null ? null
                : com.alok.monthlydashboard.entity.enums.IntervalUnit.valueOf(request.intervalUnit().name()));
        rule.setWeekday(request.weekday() == null ? null
                : com.alok.monthlydashboard.entity.enums.Weekday.valueOf(request.weekday().name()));
        rule.setWeekOfMonth(request.weekOfMonth() == null ? null
                : com.alok.monthlydashboard.entity.enums.WeekOfMonth.valueOf(request.weekOfMonth().name()));
        rule.setFallbackToLastDay(request.fallbackToLastDay() == null || request.fallbackToLastDay());
    }
}