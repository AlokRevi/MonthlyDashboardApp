import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
  } from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import {
  MonthlyDashboardResponse,
  TodayChecklistResponse,
  CreateCategoryRequest,
  CategoryResponse,
  CreateTaskRequest,
  UpdateTaskRequest,
  TaskResponse,
  RecurrenceType,
  IntervalUnit,
  DashboardCategory,
  DashboardOccurrence,
  WeekOfMonth
} from '../../models/dashboard.models';

import { DashboardApiService } from '../../services/dashboard-api.service';
import { TimelineStripComponent } from '../timeline-strip/timeline-strip.component';
import { CategorySectionComponent } from '../category-section/category-section.component';
import { TodayChecklistComponent } from '../today-checklist/today-checklist.component';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TimelineStripComponent,
    CategorySectionComponent,
    TodayChecklistComponent
  ],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardPageComponent implements OnInit {
  // --------------------------------
  // Month selector
  // --------------------------------
  monthOptions = [
    { value: 1, label: 'January' },
    { value: 2, label: 'February' },
    { value: 3, label: 'March' },
    { value: 4, label: 'April' },
    { value: 5, label: 'May' },
    { value: 6, label: 'June' },
    { value: 7, label: 'July' },
    { value: 8, label: 'August' },
    { value: 9, label: 'September' },
    { value: 10, label: 'October' },
    { value: 11, label: 'November' },
    { value: 12, label: 'December' }
  ];

  selectedYear = new Date().getFullYear();
  selectedMonth = new Date().getMonth() + 1;

  // --------------------------------
  // Recurrence dropdown options
  // Backend enum values stay unchanged.
  // --------------------------------
  recurrenceOptions: { value: RecurrenceType; label: string }[] = [
    { value: 'FIXED_DATE', label: 'Fixed Date' },
    { value: 'INTERVAL', label: 'Every X Days/Weeks' },
    { value: 'WEEKDAY', label: 'Weekday Pattern' }
  ];

  intervalUnitOptions: { value: IntervalUnit; label: string }[] = [
    { value: 'DAYS', label: 'Days' },
    { value: 'WEEKS', label: 'Weeks' }
  ];

  weekdayOptions = [
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY',
    'SUNDAY'
  ];

  weekOfMonthOptions: { value: WeekOfMonth; label: string }[] = [
    { value: 'FIRST', label: '1st' },
    { value: 'SECOND', label: '2nd' },
    { value: 'THIRD', label: '3rd' },
    { value: 'FOURTH', label: '4th' },
    { value: 'LAST', label: 'Last' }
  ];

  private getTodayString(): string {
    return new Date().toISOString().slice(0, 10);
  }

editModalOpen = false;
editTaskId: number | null = null;
editLoading = false;
editSaving = false;

editTaskCategoryId: number | null = null;
editTaskName = '';
editTaskDescription = '';
editTaskStartDate = this.getTodayString();
editTaskEndDate: string | null = null;
editTaskRecurrenceType: RecurrenceType = 'FIXED_DATE';

editTaskFixedDatesText = '';
editFallbackToLastDay = true;

editIntervalValue = 1;
editIntervalUnit: IntervalUnit = 'WEEKS';

editWeekday = 'FRIDAY';
editWeekOfMonth: WeekOfMonth = 'LAST';

openEditTaskModal(taskId: number): void {
  this.editModalOpen = true;
  this.editTaskId = taskId;
  this.editLoading = true;
  this.clearFieldErrors();

  this.dashboardApi.getTask(taskId).subscribe({
    next: (task) => {
      this.populateEditForm(task);
      this.editLoading = false;
      this.cdr.markForCheck();
    },
    error: (error) => {
      console.error('Load task failed:', error);
      this.editLoading = false;
      this.editModalOpen = false;
      this.showError('Could not load task details.');
    }
  });
}

closeEditTaskModal(): void {
  this.editModalOpen = false;
  this.editTaskId = null;
  this.editLoading = false;
  this.editSaving = false;
  this.clearFieldErrors();
}

private populateEditForm(task: TaskResponse): void {
  this.editTaskCategoryId = task.categoryId;
  this.editTaskName = task.name;
  this.editTaskDescription = task.description ?? '';
  this.editTaskStartDate = task.startDate || this.getTodayString();
  this.editTaskEndDate = task.endDate;
  this.editTaskRecurrenceType = task.recurrenceType;

  this.editTaskFixedDatesText = task.rule?.fixedDates?.join(', ') ?? '';
  this.editFallbackToLastDay = task.rule?.fallbackToLastDay ?? true;

  this.editIntervalValue = task.rule?.intervalValue ?? 1;
  this.editIntervalUnit = task.rule?.intervalUnit ?? 'WEEKS';

  this.editWeekday = task.rule?.weekday ?? 'FRIDAY';
  this.editWeekOfMonth = task.rule?.weekOfMonth ?? 'LAST';
}

private parseEditFixedDates(): number[] {
  return this.editTaskFixedDatesText
    .split(',')
    .map(value => Number(value.trim()))
    .filter(value => !Number.isNaN(value));
}

private validateEditTaskForm(): boolean {
  this.clearFieldErrors();

  if (!this.editTaskCategoryId) {
    this.setFieldError('editTaskCategory', 'Please select a category.');
  }

  if (!this.editTaskName.trim()) {
    this.setFieldError('editTaskName', 'Task name is required.');
  }

  if (!this.editTaskStartDate) {
    this.setFieldError('editTaskStartDate', 'Start date is required.');
  }

  if (this.editTaskEndDate && this.editTaskEndDate < this.editTaskStartDate) {
    this.setFieldError('editTaskEndDate', 'End date cannot be before start date.');
  }

  if (this.editTaskRecurrenceType === 'FIXED_DATE') {
    const fixedDates = this.parseEditFixedDates();

    if (fixedDates.length === 0) {
      this.setFieldError('editFixedDates', 'Enter at least one date.');
    }

    if (fixedDates.some(date => date < 1 || date > 31)) {
      this.setFieldError('editFixedDates', 'Each date must be between 1 and 31.');
    }
  }

  if (this.editTaskRecurrenceType === 'INTERVAL' && this.editIntervalValue < 1) {
    this.setFieldError('editIntervalValue', 'Interval must be at least 1.');
  }

  return Object.keys(this.fieldErrors).length === 0;
}

private buildUpdateTaskRequest(): UpdateTaskRequest {
  const baseRequest = {
    categoryId: Number(this.editTaskCategoryId),
    name: this.editTaskName.trim(),
    description: this.editTaskDescription.trim(),
    recurrenceType: this.editTaskRecurrenceType,
    startDate: this.editTaskStartDate,
    endDate: this.editTaskEndDate || null
  };

  if (this.editTaskRecurrenceType === 'FIXED_DATE') {
    return {
      ...baseRequest,
      recurrenceType: 'FIXED_DATE',
      rule: {
        fixedDates: this.parseEditFixedDates(),
        fallbackToLastDay: this.editFallbackToLastDay
      }
    };
  }

  if (this.editTaskRecurrenceType === 'INTERVAL') {
    return {
      ...baseRequest,
      recurrenceType: 'INTERVAL',
      rule: {
        intervalValue: this.editIntervalValue,
        intervalUnit: this.editIntervalUnit
      }
    };
  }

  return {
    ...baseRequest,
    recurrenceType: 'WEEKDAY',
    rule: {
      weekday: this.editWeekday,
      weekOfMonth: this.editWeekOfMonth
    }
  };
}

onUpdateTask(): void {
  if (!this.editTaskId || !this.validateEditTaskForm()) {
    this.cdr.markForCheck();
    return;
  }

  this.editSaving = true;

  this.dashboardApi.updateTask(
    this.editTaskId,
    this.buildUpdateTaskRequest()
  ).subscribe({
    next: () => {
      this.editSaving = false;
      this.closeEditTaskModal();
      this.showSuccess('Task updated.');
      this.loadDashboard();
    },
    error: (error) => {
      console.error('Update task failed:', error);
      this.editSaving = false;
      this.showError('Could not update task.');
    }
  });
}


  // --------------------------------
  // Backend data
  // --------------------------------
  dashboard: MonthlyDashboardResponse | null = null;
  checklist: TodayChecklistResponse | null = null;
  availableCategories: CategoryResponse[] = [];

  // --------------------------------
  // UI state
  // --------------------------------
  loading = false;
  errorMessage = '';
  successMessage = '';
  fieldErrors: Record<string, string> = {};

  // --------------------------------
  // Create category form
  // --------------------------------
  newCategoryName = '';
  newCategoryColor = '#2563eb';
  categorySaving = false;

  // --------------------------------
  // Create task form
  // --------------------------------
  newTaskCategoryId: number | null = null;
  newTaskName = '';
  newTaskDescription = '';
newTaskStartDate = this.getTodayString();
  newTaskEndDate: string | null = null;
  newTaskRecurrenceType: RecurrenceType = 'FIXED_DATE';
  taskSaving = false;

// Mobile/touch state for category completed-task dropdown.
expandedCategoryId: number | null = null;

toggleCategoryCompleted(categoryId: number): void {
  this.expandedCategoryId = this.expandedCategoryId === categoryId ? null : categoryId;
}

isCategoryExpanded(categoryId: number): boolean {
  return this.expandedCategoryId === categoryId;
}

/**
 * Finds completed task occurrences for the current visible month.
 * This uses the already-loaded monthly dashboard response.
 */
getCompletedTasksForCategory(categoryId: number): {
  taskName: string;
  occurrenceDate: string;
}[] {
  const category = this.dashboard?.categories.find(
    item => item.categoryId === categoryId
  );

  if (!category) {
    return [];
  }

  const completed: { taskName: string; occurrenceDate: string }[] = [];

  category.tasks.forEach(task => {
    task.occurrences
      .filter(occurrence => occurrence.status === 'COMPLETED')
      .forEach(occurrence => {
        completed.push({
          taskName: task.taskName,
          occurrenceDate: occurrence.occurrenceDate
        });
      });
  });

  return completed.sort((a, b) =>
    a.occurrenceDate.localeCompare(b.occurrenceDate)
  );
}



  // Fixed-date recurrence supports multiple dates: "8, 21"
  newTaskFixedDatesText = '';
  fallbackToLastDay = true;

  // Interval recurrence
  intervalValue = 1;
  intervalUnit: IntervalUnit = 'WEEKS';

  // Weekday recurrence
  weekday = 'FRIDAY';
  weekOfMonth: WeekOfMonth = 'LAST';

  constructor(
    private dashboardApi: DashboardApiService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  // --------------------------------
  // Computed UI values
  // --------------------------------

  // Counts overdue items from today's checklist.
  get overdueCount(): number {
    return this.checklist?.items.filter(item => item.status === 'OVERDUE').length ?? 0;
  }

  // Counts due-today items from today's checklist.
  get dueTodayCount(): number {
    return this.checklist?.items.filter(item => item.status === 'DUE_TODAY').length ?? 0;
  }

  // Hides empty categories from the main occurrence grid.
  // Empty categories still appear in the sidebar so they can be deleted.
  get visibleDashboardCategories(): DashboardCategory[] {
    return this.dashboard?.categories.filter(category => category.tasks.length > 0) ?? [];
  }

  // --------------------------------
  // Message helpers
  // --------------------------------
  private showSuccess(message: string): void {
    this.successMessage = message;
    this.errorMessage = '';
    this.cdr.markForCheck();

    setTimeout(() => {
      this.successMessage = '';
      this.cdr.markForCheck();
    }, 2500);
  }

  private showError(message: string): void {
    this.errorMessage = message;
    this.successMessage = '';
    this.cdr.markForCheck();
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  private clearFieldErrors(): void {
    this.fieldErrors = {};
  }

  private setFieldError(fieldName: string, message: string): void {
    this.fieldErrors[fieldName] = message;
  }

  // --------------------------------
  // Load dashboard data
  // --------------------------------
  loadDashboard(): void {
    this.loading = true;
    this.clearMessages();

    forkJoin({
      dashboard: this.dashboardApi.getMonthlyDashboard(this.selectedYear, this.selectedMonth),
      checklist: this.dashboardApi.getTodayChecklist(),
      categories: this.dashboardApi.getCategories()
    }).subscribe({
      next: ({ dashboard, checklist, categories }) => {
        this.dashboard = dashboard;
        this.checklist = checklist;
        this.availableCategories = categories;

        if (!this.newTaskCategoryId && categories.length > 0) {
          this.newTaskCategoryId = categories[0].id;
        }

        this.loading = false;
        this.cdr.markForCheck();
      },
     error: (error) => {
       console.error('Dashboard load failed:', error);
       this.loading = false;

       const isFreshStart =
         this.availableCategories.length === 0 &&
         !this.dashboard;

       if (isFreshStart) {
         this.showError('No dashboard data yet. Create your first category to get started.');
       } else {
         this.showError('Could not load dashboard.');
       }

       this.cdr.markForCheck();
     }
    });
  }

  // --------------------------------
  // Month navigation
  // --------------------------------
  onMonthChanged(): void {
    this.loadDashboard();
  }

  goToPreviousMonth(): void {
    if (this.selectedMonth === 1) {
      this.selectedMonth = 12;
      this.selectedYear--;
    } else {
      this.selectedMonth--;
    }

    this.loadDashboard();
  }

  goToNextMonth(): void {
    if (this.selectedMonth === 12) {
      this.selectedMonth = 1;
      this.selectedYear++;
    } else {
      this.selectedMonth++;
    }

    this.loadDashboard();
  }

  goToCurrentMonth(): void {
    const today = new Date();
    this.selectedYear = today.getFullYear();
    this.selectedMonth = today.getMonth() + 1;
    this.loadDashboard();
  }

  // --------------------------------
  // Completion actions
  // --------------------------------
  onMarkComplete(event: { taskId: number; occurrenceDate: string }): void {
    const today = new Date().toISOString().slice(0, 10);

    this.dashboardApi.completeTask(event.taskId, {
      occurrenceDate: event.occurrenceDate,
      completionDate: today
    }).subscribe({
      next: () => {
        this.showSuccess('Task marked complete.');
        this.loadDashboard();
      },
      error: (error) => {
        console.error('Complete task failed:', error);
        this.showError('Could not mark task complete.');
      }
    });
  }

  onUndoComplete(event: { taskId: number; occurrenceDate: string }): void {
    const confirmed = confirm('Undo this completion? This will change the task back to incomplete.');

    if (!confirmed) {
      return;
    }

    this.dashboardApi.undoCompletion(event.taskId, event.occurrenceDate).subscribe({
      next: () => {
        this.showSuccess('Completion undone.');
        this.loadDashboard();
      },
      error: (error) => {
        console.error('Undo completion failed:', error);
        this.showError('Could not undo completion.');
      }
    });
  }

  // --------------------------------
  // Category actions
  // --------------------------------
  onCreateCategory(): void {
    this.clearFieldErrors();

    const name = this.newCategoryName.trim();

    if (!name) {
      this.setFieldError('categoryName', 'Category name is required.');
      return;
    }

    this.categorySaving = true;

    const request: CreateCategoryRequest = {
      name,
      color: this.newCategoryColor || '#2563eb'
    };

    this.dashboardApi.createCategory(request).subscribe({
      next: (createdCategory) => {
        this.newCategoryName = '';
        this.newCategoryColor = '#2563eb';
        this.categorySaving = false;
        this.newTaskCategoryId = createdCategory.id;

        this.showSuccess('Category created.');
        this.loadDashboard();
      },
      error: (error) => {
        console.error('Create category failed:', error);
        this.categorySaving = false;
        this.showError('Could not create category.');
      }
    });
  }

  onDeleteCategory(categoryId: number): void {
    const confirmed = confirm('Delete this category? This only works if it has no tasks.');

    if (!confirmed) {
      return;
    }

    this.dashboardApi.deleteCategory(categoryId).subscribe({
      next: () => {
        this.showSuccess('Category deleted.');
        this.loadDashboard();
      },
      error: (error) => {
        console.error('Delete category failed:', error);
        this.showError('Could not delete category. It may still contain tasks.');
      }
    });
  }

  // --------------------------------
  // Task form helpers
  // --------------------------------
  private parseFixedDates(): number[] {
    return this.newTaskFixedDatesText
      .split(',')
      .map(value => Number(value.trim()))
      .filter(value => !Number.isNaN(value));
  }

  private validateTaskForm(): boolean {
    this.clearFieldErrors();

    if (!this.newTaskCategoryId) {
      this.setFieldError('taskCategory', 'Please select a category.');
    }

    if (!this.newTaskName.trim()) {
      this.setFieldError('taskName', 'Task name is required.');
    }

    if (!this.newTaskStartDate) {
      this.setFieldError('taskStartDate', 'Start date is required.');
    }

    if (this.newTaskEndDate && this.newTaskEndDate < this.newTaskStartDate) {
      this.setFieldError('taskEndDate', 'End date cannot be before start date.');
    }

    if (this.newTaskRecurrenceType === 'FIXED_DATE') {
      const fixedDates = this.parseFixedDates();

      if (fixedDates.length === 0) {
        this.setFieldError('fixedDates', 'Enter at least one date.');
      }

      if (fixedDates.some(date => date < 1 || date > 31)) {
        this.setFieldError('fixedDates', 'Each date must be between 1 and 31.');
      }
    }

    if (this.newTaskRecurrenceType === 'INTERVAL' && this.intervalValue < 1) {
      this.setFieldError('intervalValue', 'Interval must be at least 1.');
    }

    if (this.newTaskRecurrenceType === 'WEEKDAY' && !this.weekOfMonth) {
      this.setFieldError('weekOfMonth', 'Choose 1st, 2nd, 3rd, 4th, or Last.');
    }

    const hasErrors = Object.keys(this.fieldErrors).length > 0;

    if (hasErrors) {
      this.showError('Please fix the highlighted fields.');
    }

    return !hasErrors;
  }

  private buildCreateTaskRequest(): CreateTaskRequest {
    const baseRequest = {
      categoryId: Number(this.newTaskCategoryId),
      name: this.newTaskName.trim(),
      description: this.newTaskDescription.trim(),
      recurrenceType: this.newTaskRecurrenceType,
      startDate: this.newTaskStartDate,
      endDate: this.newTaskEndDate || null
    };

    if (this.newTaskRecurrenceType === 'FIXED_DATE') {
      return {
        ...baseRequest,
        recurrenceType: 'FIXED_DATE',
        rule: {
          fixedDates: this.parseFixedDates(),
          fallbackToLastDay: this.fallbackToLastDay
        }
      };
    }

    if (this.newTaskRecurrenceType === 'INTERVAL') {
      return {
        ...baseRequest,
        recurrenceType: 'INTERVAL',
        rule: {
          intervalValue: this.intervalValue,
          intervalUnit: this.intervalUnit
        }
      };
    }

    return {
      ...baseRequest,
      recurrenceType: 'WEEKDAY',
      rule: {
        weekday: this.weekday,
        weekOfMonth: this.weekOfMonth
      }
    };
  }

  // --------------------------------
  // Task actions
  // --------------------------------
  onCreateTask(): void {
    if (!this.validateTaskForm()) {
      this.cdr.markForCheck();
      return;
    }

    this.taskSaving = true;

    const request = this.buildCreateTaskRequest();

    this.dashboardApi.createTask(request).subscribe({
      next: () => {
        this.newTaskName = '';
        this.newTaskDescription = '';
        this.newTaskFixedDatesText = '';
        this.fallbackToLastDay = true;
        this.intervalValue = 1;
        this.intervalUnit = 'WEEKS';
        this.weekday = 'FRIDAY';
        this.weekOfMonth = 'LAST';
        this.taskSaving = false;

        this.showSuccess('Task created.');
        this.loadDashboard();
      },
      error: (error) => {
        console.error('Create task failed:', error);
        this.taskSaving = false;
        this.showError('Could not create task.');
      }
    });
  }

  onDeleteTask(taskId: number): void {
    const confirmed = confirm('Delete this task?');

    if (!confirmed) {
      return;
    }

    this.dashboardApi.deleteTask(taskId).subscribe({
      next: () => {
        this.showSuccess('Task deleted.');
        this.loadDashboard();
      },
      error: (error) => {
        console.error('Delete task failed:', error);
        this.showError('Could not delete task.');
      }
    });
  }
}
