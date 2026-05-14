package com.alok.monthlydashboard.service.impl;

import com.alok.monthlydashboard.common.enums.CategoryRequires;
import com.alok.monthlydashboard.common.enums.FeelsLikeLabel;
import com.alok.monthlydashboard.common.enums.RecurrenceType;
import com.alok.monthlydashboard.common.enums.SetupImportMode;
import com.alok.monthlydashboard.dto.importsetup.SetupImportCategoryRequest;
import com.alok.monthlydashboard.dto.importsetup.SetupImportPreviewResponse;
import com.alok.monthlydashboard.dto.importsetup.SetupImportRequest;
import com.alok.monthlydashboard.dto.importsetup.SetupImportResultResponse;
import com.alok.monthlydashboard.dto.importsetup.SetupImportRuleRequest;
import com.alok.monthlydashboard.dto.importsetup.SetupImportTaskRequest;
import com.alok.monthlydashboard.entity.Category;
import com.alok.monthlydashboard.entity.Task;
import com.alok.monthlydashboard.entity.TaskFixedDate;
import com.alok.monthlydashboard.entity.TaskRecurrenceRule;
import com.alok.monthlydashboard.exception.ConflictException;
import com.alok.monthlydashboard.exception.ValidationException;
import com.alok.monthlydashboard.repository.CategoryRepository;
import com.alok.monthlydashboard.repository.TaskRepository;
import com.alok.monthlydashboard.service.SetupImportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SetupImportServiceImpl implements SetupImportService {

    private static final String SUPPORTED_VERSION = "v2-setup-snapshot";

    private final CategoryRepository categoryRepository;
    private final TaskRepository taskRepository;

    public SetupImportServiceImpl(
            CategoryRepository categoryRepository,
            TaskRepository taskRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SetupImportPreviewResponse previewSetupImport(SetupImportRequest request) {
        ImportValidationResult validation = validate(request);

        return new SetupImportPreviewResponse(
                validation.errors().isEmpty(),
                request == null ? null : request.version(),
                request == null || request.categories() == null ? 0 : request.categories().size(),
                request == null || request.tasks() == null ? 0 : request.tasks().size(),
                countActiveTasks(request),
                countInactiveTasks(request),
                validation.warnings(),
                validation.errors()
        );
    }

    @Override
    @Transactional
    public SetupImportResultResponse importSetup(SetupImportRequest request, SetupImportMode mode) {
        if (mode != SetupImportMode.EMPTY_ONLY) {
            throw new ValidationException("Only EMPTY_ONLY setup import mode is supported");
        }

        ImportValidationResult validation = validate(request);
        if (!validation.errors().isEmpty()) {
            throw new ValidationException("Setup snapshot is invalid: " + String.join("; ", validation.errors()));
        }

        if (categoryRepository.count() > 0 || taskRepository.count() > 0) {
            throw new ConflictException("Setup import requires an empty setup. Existing categories or tasks were found.");
        }

        Map<Long, Category> categoryByOldId = new HashMap<>();
        for (SetupImportCategoryRequest categoryRequest : request.categories()) {
            Category category = new Category();
            category.setName(categoryRequest.name().trim());
            category.setColor(categoryRequest.color());
            category.setRequires(parseEnumOrNull(CategoryRequires.class, categoryRequest.requires()));
            category.setFeelsLike(parseFeelsLikeList(categoryRequest.feelsLike()));

            Category saved = categoryRepository.save(category);
            categoryByOldId.put(categoryRequest.id(), saved);
        }

        for (SetupImportTaskRequest taskRequest : request.tasks()) {
            Task task = new Task();
            task.setCategory(categoryByOldId.get(taskRequest.categoryId()));
            task.setName(taskRequest.name().trim());
            task.setDescription(taskRequest.description());
            task.setRecurrenceType(parseEnumOrNull(RecurrenceType.class, taskRequest.recurrenceType()));
            task.setStartDate(parseDate(taskRequest.startDate()));
            task.setEndDate(parseNullableDate(taskRequest.endDate()));
            task.setActive(taskRequest.isActive() == null || taskRequest.isActive());
            task.setEnergyOverride(parseEnumOrNull(FeelsLikeLabel.class, taskRequest.energyOverride()));
            task.setEnjoymentOverride(parseEnumOrNull(FeelsLikeLabel.class, taskRequest.enjoymentOverride()));
            task.setPressureOverride(parseEnumOrNull(FeelsLikeLabel.class, taskRequest.pressureOverride()));
            task.setEffortOverride(parseEnumOrNull(FeelsLikeLabel.class, taskRequest.effortOverride()));

            TaskRecurrenceRule rule = buildRule(taskRequest.rule());
            task.setRecurrenceRule(rule);

            if (task.getRecurrenceType() == RecurrenceType.FIXED_DATE) {
                for (Integer day : taskRequest.rule().fixedDates()) {
                    TaskFixedDate fixedDate = new TaskFixedDate();
                    fixedDate.setDayOfMonth(day);
                    task.addFixedDate(fixedDate);
                }
            }

            taskRepository.save(task);
        }

        return new SetupImportResultResponse(
                true,
                mode.name(),
                request.categories().size(),
                request.tasks().size(),
                countActiveTasks(request),
                countInactiveTasks(request),
                validation.warnings(),
                List.of()
        );
    }

    private ImportValidationResult validate(SetupImportRequest request) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("Snapshot body is required.");
            return new ImportValidationResult(warnings, errors);
        }

        if (!SUPPORTED_VERSION.equals(request.version())) {
            errors.add("version must be v2-setup-snapshot.");
        }

        if (request.categories() == null) {
            errors.add("categories is required.");
        }

        if (request.tasks() == null) {
            errors.add("tasks is required.");
        }

        if (!errors.isEmpty()) {
            return new ImportValidationResult(warnings, errors);
        }

        Set<Long> categoryIds = validateCategories(request.categories(), errors);
        validateTasks(request.tasks(), categoryIds, errors);
        validateTaskCountWarnings(request, warnings);

        return new ImportValidationResult(warnings, errors);
    }

    private Set<Long> validateCategories(List<SetupImportCategoryRequest> categories, List<String> errors) {
        Set<Long> categoryIds = new HashSet<>();

        for (int index = 0; index < categories.size(); index++) {
            SetupImportCategoryRequest category = categories.get(index);
            String path = "categories[" + index + "]";

            if (category.id() == null) {
                errors.add(path + ".id is required.");
            } else if (!categoryIds.add(category.id())) {
                errors.add(path + ".id is duplicated.");
            }

            if (category.name() == null || category.name().trim().isEmpty()) {
                errors.add(path + ".name is required.");
            } else if (category.name().length() > 100) {
                errors.add(path + ".name must be at most 100 characters.");
            }

            if (category.color() != null && category.color().length() > 20) {
                errors.add(path + ".color must be at most 20 characters.");
            }

            validateEnum(CategoryRequires.class, category.requires(), path + ".requires", errors, false);
            validateFeelsLikeList(category.feelsLike(), path + ".feelsLike", errors);
        }

        return categoryIds;
    }

    private void validateTasks(
            List<SetupImportTaskRequest> tasks,
            Set<Long> categoryIds,
            List<String> errors
    ) {
        for (int index = 0; index < tasks.size(); index++) {
            SetupImportTaskRequest task = tasks.get(index);
            String path = "tasks[" + index + "]";

            if (task.categoryId() == null || !categoryIds.contains(task.categoryId())) {
                errors.add(path + ".categoryId must reference a snapshot category.");
            }

            if (task.name() == null || task.name().trim().isEmpty()) {
                errors.add(path + ".name is required.");
            } else if (task.name().length() > 150) {
                errors.add(path + ".name must be at most 150 characters.");
            }

            RecurrenceType recurrenceType = validateEnum(
                    RecurrenceType.class,
                    task.recurrenceType(),
                    path + ".recurrenceType",
                    errors,
                    true
            );

            LocalDate startDate = validateDate(task.startDate(), path + ".startDate", errors, true);
            LocalDate endDate = validateDate(task.endDate(), path + ".endDate", errors, false);
            if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                errors.add(path + ".endDate cannot be before startDate.");
            }

            validateEnum(FeelsLikeLabel.class, task.energyOverride(), path + ".energyOverride", errors, false);
            validateEnum(FeelsLikeLabel.class, task.enjoymentOverride(), path + ".enjoymentOverride", errors, false);
            validateEnum(FeelsLikeLabel.class, task.pressureOverride(), path + ".pressureOverride", errors, false);
            validateEnum(FeelsLikeLabel.class, task.effortOverride(), path + ".effortOverride", errors, false);
            validateRule(task.rule(), recurrenceType, path + ".rule", errors);
        }
    }

    private void validateRule(
            SetupImportRuleRequest rule,
            RecurrenceType recurrenceType,
            String path,
            List<String> errors
    ) {
        if (recurrenceType == null) {
            return;
        }

        if (rule == null) {
            errors.add(path + " is required.");
            return;
        }

        if (recurrenceType == RecurrenceType.FIXED_DATE) {
            validateFixedDateRule(rule.fixedDates(), path + ".fixedDates", errors);
        }

        if (recurrenceType == RecurrenceType.INTERVAL) {
            if (rule.intervalValue() == null || rule.intervalValue() <= 0) {
                errors.add(path + ".intervalValue must be greater than 0 for INTERVAL tasks.");
            }
            validateEnum(com.alok.monthlydashboard.entity.enums.IntervalUnit.class, rule.intervalUnit(), path + ".intervalUnit", errors, true);
        }

        if (recurrenceType == RecurrenceType.WEEKDAY) {
            validateEnum(com.alok.monthlydashboard.entity.enums.Weekday.class, rule.weekday(), path + ".weekday", errors, true);
            validateEnum(com.alok.monthlydashboard.entity.enums.WeekOfMonth.class, rule.weekOfMonth(), path + ".weekOfMonth", errors, true);
        }
    }

    private void validateFixedDateRule(List<Integer> fixedDates, String path, List<String> errors) {
        if (fixedDates == null || fixedDates.isEmpty()) {
            errors.add(path + " must contain at least one day for FIXED_DATE tasks.");
            return;
        }

        Set<Integer> uniqueDays = new HashSet<>();
        for (Integer day : fixedDates) {
            if (day == null || day < 1 || day > 31) {
                errors.add(path + " values must be between 1 and 31.");
                return;
            }
            if (!uniqueDays.add(day)) {
                errors.add(path + " must not contain duplicate values.");
                return;
            }
        }
    }

    private void validateTaskCountWarnings(SetupImportRequest request, List<String> warnings) {
        Map<Long, Integer> actualCounts = new HashMap<>();
        for (SetupImportTaskRequest task : request.tasks()) {
            if (task.categoryId() != null) {
                actualCounts.merge(task.categoryId(), 1, Integer::sum);
            }
        }

        for (SetupImportCategoryRequest category : request.categories()) {
            if (category.id() != null
                    && category.taskCount() != null
                    && category.taskCount() != actualCounts.getOrDefault(category.id(), 0)) {
                warnings.add("categories[" + category.id() + "].taskCount does not match referenced tasks and will be ignored.");
            }
        }
    }

    private TaskRecurrenceRule buildRule(SetupImportRuleRequest request) {
        TaskRecurrenceRule rule = new TaskRecurrenceRule();
        rule.setFallbackToLastDay(request.fallbackToLastDay() == null || request.fallbackToLastDay());
        rule.setIntervalValue(request.intervalValue());
        rule.setIntervalUnit(parseEnumOrNull(com.alok.monthlydashboard.entity.enums.IntervalUnit.class, request.intervalUnit()));
        rule.setWeekday(parseEnumOrNull(com.alok.monthlydashboard.entity.enums.Weekday.class, request.weekday()));
        rule.setWeekOfMonth(parseEnumOrNull(com.alok.monthlydashboard.entity.enums.WeekOfMonth.class, request.weekOfMonth()));

        return rule;
    }

    private List<FeelsLikeLabel> parseFeelsLikeList(List<String> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
                .map(value -> parseEnumOrNull(FeelsLikeLabel.class, value))
                .toList();
    }

    private void validateFeelsLikeList(List<String> values, String path, List<String> errors) {
        if (values == null) {
            return;
        }

        for (int index = 0; index < values.size(); index++) {
            validateEnum(FeelsLikeLabel.class, values.get(index), path + "[" + index + "]", errors, true);
        }
    }

    private <T extends Enum<T>> T validateEnum(
            Class<T> enumType,
            String value,
            String path,
            List<String> errors,
            boolean required
    ) {
        if (value == null || value.isBlank()) {
            if (required) {
                errors.add(path + " is required.");
            }
            return null;
        }

        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException ex) {
            errors.add(path + " is invalid: " + value + ".");
            return null;
        }
    }

    private LocalDate validateDate(String value, String path, List<String> errors, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) {
                errors.add(path + " is required.");
            }
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            errors.add(path + " must be a valid ISO date.");
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        return LocalDate.parse(value);
    }

    private LocalDate parseNullableDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    private <T extends Enum<T>> T parseEnumOrNull(Class<T> enumType, String value) {
        return value == null || value.isBlank() ? null : Enum.valueOf(enumType, value);
    }

    private int countActiveTasks(SetupImportRequest request) {
        if (request == null || request.tasks() == null) {
            return 0;
        }

        return (int) request.tasks().stream()
                .filter(task -> task.isActive() == null || task.isActive())
                .count();
    }

    private int countInactiveTasks(SetupImportRequest request) {
        if (request == null || request.tasks() == null) {
            return 0;
        }

        return (int) request.tasks().stream()
                .filter(task -> task.isActive() != null && !task.isActive())
                .count();
    }

    private record ImportValidationResult(List<String> warnings, List<String> errors) {
    }
}
