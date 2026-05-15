import { computed, Injectable, signal } from '@angular/core';
import { catchError, forkJoin, of } from 'rxjs';

import {
  CategoryResponse,
  CreateCategoryRequest,
  CreateTaskRequest,
  MonthlyDashboardResponse,
  SetupImportPreviewResponse,
  TaskResponse,
  TodayChecklistResponse,
  UpdateCategoryRequest,
  UpdateTaskRequest,
  ScaleNumbering,
  StartOfWeek,
  TimelineDashboardResponse,
  TimelineView,
  ViewSettings
} from '../../models/dashboard.models';
import { DashboardApiService } from '../../services/dashboard-api.service';
import { DateFormatService } from '../../services/date-format.service';

@Injectable()
export class DashboardPageStateService {
  selectedYear = signal(new Date().getFullYear());
  selectedMonth = signal(new Date().getMonth() + 1);
  viewSettings = signal<ViewSettings>({
    view: 'MONTH',
    startOfWeek: 'SUNDAY',
    scaleNumbering: 'SEGMENT',
    calendarYearBound: true
  });

  dashboard = signal<MonthlyDashboardResponse | null>(null);
  monthTimelineDashboard = signal<TimelineDashboardResponse | null>(null);
  checklist = signal<TodayChecklistResponse | null>(null);
  availableCategories = signal<CategoryResponse[]>([]);

  loading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  taskSaving = signal(false);
  taskCreateSuccessCount = signal(0);
  taskCreateScreenOpen = signal(false);
  taskCreateCategoryId = signal<number | null>(null);
  categorySaving = signal(false);
  categoryCreateDialogOpen = signal(false);
  categoryBeingEdited = signal<CategoryResponse | null>(null);
  setupImportPreview = signal<SetupImportPreviewResponse | null>(null);
  setupImportSnapshot = signal<unknown | null>(null);
  setupImportPreviewing = signal(false);
  setupImporting = signal(false);

  editModalOpen = signal(false);
  editTaskId = signal<number | null>(null);
  editOccurrenceDate = signal<string | null>(null);
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
      monthTimelineDashboard: this.dashboardApi.getMonthTimelineDashboard().pipe(
        catchError(() => of(null))
      ),
      checklist: this.dashboardApi.getTodayChecklist(),
      categories: this.dashboardApi.getCategories()
    }).subscribe({
      next: ({ dashboard, monthTimelineDashboard, checklist, categories }) => {
        this.dashboard.set(dashboard);
        this.monthTimelineDashboard.set(monthTimelineDashboard);
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

  setTimelineView(view: TimelineView): void {
    this.viewSettings.update(settings => ({
      ...settings,
      view
    }));
  }

  setStartOfWeek(startOfWeek: StartOfWeek): void {
    this.viewSettings.update(settings => ({
      ...settings,
      startOfWeek
    }));
  }

  setScaleNumbering(scaleNumbering: ScaleNumbering): void {
    this.viewSettings.update(settings => ({
      ...settings,
      scaleNumbering
    }));
  }

  setCalendarYearBound(calendarYearBound: boolean): void {
    this.viewSettings.update(settings => ({
      ...settings,
      calendarYearBound
    }));
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
        this.taskCreateSuccessCount.update(count => count + 1);
        this.closeTaskCreateScreen();
        this.showSuccess('Task created.');
        this.loadDashboard();
      },
      error: (error) => {
        console.error('Create task failed:', error);
        this.taskSaving.set(false);
        this.showError(this.buildApiErrorMessage('Could not create task', error));
      }
    });
  }

  openTaskCreateScreen(categoryId: number | null = null): void {
    this.taskCreateCategoryId.set(categoryId);
    this.taskCreateScreenOpen.set(true);
  }

  closeTaskCreateScreen(): void {
    this.taskCreateScreenOpen.set(false);
    this.taskCreateCategoryId.set(null);
  }

  createCategory(request: CreateCategoryRequest): void {
    this.categorySaving.set(true);

    this.dashboardApi.createCategory(request).subscribe({
      next: () => {
        this.categorySaving.set(false);
        this.closeCategoryCreateDialog();
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

  updateCategory(event: { categoryId: number; request: UpdateCategoryRequest }): void {
    this.categorySaving.set(true);

    this.dashboardApi.updateCategory(event.categoryId, event.request).subscribe({
      next: () => {
        this.categorySaving.set(false);
        this.closeCategoryCreateDialog();
        this.showSuccess('Category updated.');
        this.loadDashboard();
      },
      error: (error) => {
        console.error('Update category failed:', error);
        this.categorySaving.set(false);
        this.showError(this.buildApiErrorMessage('Could not update category', error));
      }
    });
  }

  openCategoryCreateDialog(): void {
    this.categoryBeingEdited.set(null);
    this.categoryCreateDialogOpen.set(true);
  }

  openCategoryEditDialog(categoryId: number): void {
    const category = this.availableCategories().find(item => item.id === categoryId);

    if (!category) {
      this.showError('Could not load category details.');
      return;
    }

    this.categoryBeingEdited.set(category);
    this.categoryCreateDialogOpen.set(true);
  }

  closeCategoryCreateDialog(): void {
    this.categoryCreateDialogOpen.set(false);
    this.categoryBeingEdited.set(null);
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

  openEditTaskModal(event: number | { taskId: number; occurrenceDate: string | null }): void {
    const taskId = typeof event === 'number' ? event : event.taskId;
    const occurrenceDate = typeof event === 'number' ? null : event.occurrenceDate;

    this.editModalOpen.set(true);
    this.editTaskId.set(taskId);
    this.editOccurrenceDate.set(occurrenceDate);
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
    this.editOccurrenceDate.set(null);
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

  exportSetupSnapshot(): void {
    this.dashboardApi.getSetupSnapshot().subscribe({
      next: (snapshot) => {
        this.downloadJsonSnapshot(snapshot);
        this.showSuccess('Setup snapshot exported.');
      },
      error: (error) => {
        console.error('Export setup snapshot failed:', error);
        this.showError('Could not export setup snapshot.');
      }
    });
  }

  previewSetupImportFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';

    if (!file) {
      return;
    }

    if (!file.name.toLowerCase().endsWith('.json')) {
      this.showError('Choose a JSON setup snapshot file.');
      return;
    }

    this.setupImportPreviewing.set(true);
    this.setupImportPreview.set(null);
    this.setupImportSnapshot.set(null);

    file.text()
      .then(text => {
        const snapshot = JSON.parse(text) as unknown;

        this.dashboardApi.previewSetupImport(snapshot).subscribe({
          next: (preview) => {
            this.setupImportPreviewing.set(false);
            this.setupImportPreview.set(preview);
            this.setupImportSnapshot.set(snapshot);
            this.clearMessages();
          },
          error: (error) => {
            console.error('Preview setup import failed:', error);
            this.setupImportPreviewing.set(false);
            this.showError(this.buildApiErrorMessage('Could not preview setup snapshot', error));
          }
        });
      })
      .catch(error => {
        console.error('Read setup import file failed:', error);
        this.setupImportPreviewing.set(false);
        this.showError('Could not read setup snapshot file.');
      });
  }

  importSetupSnapshot(): void {
    const snapshot = this.setupImportSnapshot();
    const preview = this.setupImportPreview();

    if (!snapshot || !preview?.valid) {
      this.showError('Preview a valid setup snapshot before importing.');
      return;
    }

    this.setupImporting.set(true);

    this.dashboardApi.importSetupSnapshot(snapshot).subscribe({
      next: () => {
        this.setupImporting.set(false);
        this.setupImportPreview.set(null);
        this.setupImportSnapshot.set(null);
        this.showSuccess('Setup snapshot imported.');
        this.loadDashboard();
      },
      error: (error) => {
        console.error('Import setup snapshot failed:', error);
        this.setupImporting.set(false);
        this.showError(this.buildApiErrorMessage('Could not import setup snapshot', error));
      }
    });
  }

  clearSetupImportPreview(): void {
    this.setupImportPreview.set(null);
    this.setupImportSnapshot.set(null);
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

  private buildApiErrorMessage(prefix: string, error: unknown): string {
    const apiError = error as { error?: { message?: string; details?: string[] } };
    const message = apiError.error?.message;
    const details = apiError.error?.details?.filter(Boolean).join(' ');

    if (message && details) {
      return `${prefix}: ${message} ${details}`;
    }

    if (message) {
      return `${prefix}: ${message}`;
    }

    return `${prefix}.`;
  }

  private downloadJsonSnapshot(snapshot: unknown): void {
    const today = this.dateFormat.toIsoDate();
    const fileName = `monthly-dashboard-setup-snapshot-${today}.json`;
    const blob = new Blob([JSON.stringify(snapshot, null, 2)], {
      type: 'application/json'
    });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');

    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
