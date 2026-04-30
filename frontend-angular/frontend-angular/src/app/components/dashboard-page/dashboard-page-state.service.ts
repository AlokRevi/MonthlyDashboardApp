import { computed, Injectable, signal } from '@angular/core';
import { forkJoin } from 'rxjs';

import {
  CategoryResponse,
  CreateCategoryRequest,
  CreateTaskRequest,
  MonthlyDashboardResponse,
  TaskResponse,
  TodayChecklistResponse,
  UpdateTaskRequest
} from '../../models/dashboard.models';
import { DashboardApiService } from '../../services/dashboard-api.service';
import { DateFormatService } from '../../services/date-format.service';

@Injectable()
export class DashboardPageStateService {
  selectedYear = signal(new Date().getFullYear());
  selectedMonth = signal(new Date().getMonth() + 1);

  dashboard = signal<MonthlyDashboardResponse | null>(null);
  checklist = signal<TodayChecklistResponse | null>(null);
  availableCategories = signal<CategoryResponse[]>([]);

  loading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  taskSaving = signal(false);
  categorySaving = signal(false);

  editModalOpen = signal(false);
  editTaskId = signal<number | null>(null);
  editTask = signal<TaskResponse | null>(null);
  editLoading = signal(false);
  editSaving = signal(false);

  overdueCount = computed(() =>
    this.checklist()?.items.filter(item => item.status === 'OVERDUE').length ?? 0
  );

  dueTodayCount = computed(() =>
    this.checklist()?.items.filter(item => item.status === 'DUE_TODAY').length ?? 0
  );

  visibleDashboardCategories = computed(() =>
    this.dashboard()?.categories.filter(category => category.tasks.length > 0) ?? []
  );

  constructor(
    private dashboardApi: DashboardApiService,
    private dateFormat: DateFormatService
  ) {}

  loadDashboard(): void {
    this.loading.set(true);
    this.clearMessages();

    forkJoin({
      dashboard: this.dashboardApi.getMonthlyDashboard(this.selectedYear(), this.selectedMonth()),
      checklist: this.dashboardApi.getTodayChecklist(),
      categories: this.dashboardApi.getCategories()
    }).subscribe({
      next: ({ dashboard, checklist, categories }) => {
        this.dashboard.set(dashboard);
        this.checklist.set(checklist);
        this.availableCategories.set(categories);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Dashboard load failed:', error);
        this.loading.set(false);
        this.showError(
          this.availableCategories().length === 0 && !this.dashboard()
            ? 'No dashboard data yet. Create your first category to get started.'
            : 'Could not load dashboard.'
        );
      }
    });
  }

  setSelectedYear(year: number): void {
    this.selectedYear.set(year);
  }

  setSelectedMonth(month: number): void {
    this.selectedMonth.set(month);
  }

  goToPreviousMonth(): void {
    if (this.selectedMonth() === 1) {
      this.selectedMonth.set(12);
      this.selectedYear.update(year => year - 1);
    } else {
      this.selectedMonth.update(month => month - 1);
    }

    this.loadDashboard();
  }

  goToNextMonth(): void {
    if (this.selectedMonth() === 12) {
      this.selectedMonth.set(1);
      this.selectedYear.update(year => year + 1);
    } else {
      this.selectedMonth.update(month => month + 1);
    }

    this.loadDashboard();
  }

  goToCurrentMonth(): void {
    const today = new Date();
    this.selectedYear.set(today.getFullYear());
    this.selectedMonth.set(today.getMonth() + 1);
    this.loadDashboard();
  }

  createTask(request: CreateTaskRequest): void {
    this.taskSaving.set(true);

    this.dashboardApi.createTask(request).subscribe({
      next: () => {
        this.taskSaving.set(false);
        this.showSuccess('Task created.');
        this.loadDashboard();
      },
      error: (error) => {
        console.error('Create task failed:', error);
        this.taskSaving.set(false);
        this.showError('Could not create task.');
      }
    });
  }

  createCategory(request: CreateCategoryRequest): void {
    this.categorySaving.set(true);

    this.dashboardApi.createCategory(request).subscribe({
      next: () => {
        this.categorySaving.set(false);
        this.showSuccess('Category created.');
        this.loadDashboard();
      },
      error: (error) => {
        console.error('Create category failed:', error);
        this.categorySaving.set(false);
        this.showError('Could not create category.');
      }
    });
  }

  deleteCategory(categoryId: number): void {
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

  openEditTaskModal(taskId: number): void {
    this.editModalOpen.set(true);
    this.editTaskId.set(taskId);
    this.editTask.set(null);
    this.editLoading.set(true);

    this.dashboardApi.getTask(taskId).subscribe({
      next: (task) => {
        this.editTask.set(task);
        this.editLoading.set(false);
      },
      error: (error) => {
        console.error('Load task failed:', error);
        this.editLoading.set(false);
        this.closeEditTaskModal();
        this.showError('Could not load task details.');
      }
    });
  }

  closeEditTaskModal(): void {
    this.editModalOpen.set(false);
    this.editTaskId.set(null);
    this.editTask.set(null);
    this.editLoading.set(false);
    this.editSaving.set(false);
  }

  updateTask(request: UpdateTaskRequest): void {
    const editTaskId = this.editTaskId();

    if (!editTaskId) {
      return;
    }

    this.editSaving.set(true);

    this.dashboardApi.updateTask(editTaskId, request).subscribe({
      next: () => {
        this.editSaving.set(false);
        this.closeEditTaskModal();
        this.showSuccess('Task updated.');
        this.loadDashboard();
      },
      error: (error) => {
        console.error('Update task failed:', error);
        this.editSaving.set(false);
        this.showError('Could not update task.');
      }
    });
  }

  deleteTask(taskId: number): void {
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

  markComplete(event: { taskId: number; occurrenceDate: string }): void {
    this.dashboardApi.completeTask(event.taskId, {
      occurrenceDate: event.occurrenceDate,
      completionDate: this.dateFormat.toIsoDate()
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

  undoComplete(event: { taskId: number; occurrenceDate: string }): void {
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

  showError(message: string): void {
    this.errorMessage.set(message);
    this.successMessage.set('');
  }

  private showSuccess(message: string): void {
    this.successMessage.set(message);
    this.errorMessage.set('');

    setTimeout(() => {
      this.successMessage.set('');
    }, 2500);
  }

  private clearMessages(): void {
    this.errorMessage.set('');
    this.successMessage.set('');
  }
}
