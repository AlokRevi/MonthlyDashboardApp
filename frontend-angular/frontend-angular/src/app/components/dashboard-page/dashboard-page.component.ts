import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';

import {
  CategoryResponse,
  CreateCategoryRequest,
  CreateTaskRequest,
  DashboardCategory,
  IntervalUnit,
  MonthlyDashboardResponse,
  RecurrenceType,
  TaskResponse,
  TodayChecklistResponse,
  UpdateTaskRequest,
  WeekOfMonth
} from '../../models/dashboard.models';

import { DashboardApiService } from '../../services/dashboard-api.service';
import { DateFormatService } from '../../services/date-format.service';
import { TimelineStripComponent } from '../timeline-strip/timeline-strip.component';
import { CategorySectionComponent } from '../category-section/category-section.component';
import { TodayChecklistComponent } from '../today-checklist/today-checklist.component';
import { TaskCreateFormComponent } from '../task-create-form/task-create-form.component';
import { TaskEditModalComponent } from '../task-edit-modal/task-edit-modal.component';
import { CategoryManagerComponent } from '../category-manager/category-manager.component';
import { MonthNavigationComponent } from '../month-navigation/month-navigation.component';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    TimelineStripComponent,
    CategorySectionComponent,
    TodayChecklistComponent,
    TaskCreateFormComponent,
    TaskEditModalComponent,
    CategoryManagerComponent,
    MonthNavigationComponent
  ],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardPageComponent implements OnInit {
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

  selectedYear = new Date().getFullYear();
  selectedMonth = new Date().getMonth() + 1;

  dashboard: MonthlyDashboardResponse | null = null;
  checklist: TodayChecklistResponse | null = null;
  availableCategories: CategoryResponse[] = [];

  loading = false;
  errorMessage = '';
  successMessage = '';
  taskSaving = false;
  categorySaving = false;

  editModalOpen = false;
  editTaskId: number | null = null;
  editTask: TaskResponse | null = null;
  editLoading = false;
  editSaving = false;

  constructor(
    private dashboardApi: DashboardApiService,
    private dateFormat: DateFormatService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  get overdueCount(): number {
    return this.checklist?.items.filter(item => item.status === 'OVERDUE').length ?? 0;
  }

  get dueTodayCount(): number {
    return this.checklist?.items.filter(item => item.status === 'DUE_TODAY').length ?? 0;
  }

  get visibleDashboardCategories(): DashboardCategory[] {
    return this.dashboard?.categories.filter(category => category.tasks.length > 0) ?? [];
  }

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
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Dashboard load failed:', error);
        this.loading = false;
        this.showError(
          this.availableCategories.length === 0 && !this.dashboard
            ? 'No dashboard data yet. Create your first category to get started.'
            : 'Could not load dashboard.'
        );
        this.cdr.markForCheck();
      }
    });
  }

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

  onCreateTask(request: CreateTaskRequest): void {
    this.taskSaving = true;

    this.dashboardApi.createTask(request).subscribe({
      next: () => {
        this.taskSaving = false;
        this.showSuccess('Task created.');
        this.loadDashboard();
      },
      error: (error) => {
        console.error('Create task failed:', error);
        this.taskSaving = false;
        this.showError('Could not create task.');
        this.cdr.markForCheck();
      }
    });
  }

  onCreateCategory(request: CreateCategoryRequest): void {
    this.categorySaving = true;

    this.dashboardApi.createCategory(request).subscribe({
      next: () => {
        this.categorySaving = false;
        this.showSuccess('Category created.');
        this.loadDashboard();
      },
      error: (error) => {
        console.error('Create category failed:', error);
        this.categorySaving = false;
        this.showError('Could not create category.');
        this.cdr.markForCheck();
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

  openEditTaskModal(taskId: number): void {
    this.editModalOpen = true;
    this.editTaskId = taskId;
    this.editTask = null;
    this.editLoading = true;

    this.dashboardApi.getTask(taskId).subscribe({
      next: (task) => {
        this.editTask = task;
        this.editLoading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Load task failed:', error);
        this.editLoading = false;
        this.closeEditTaskModal();
        this.showError('Could not load task details.');
      }
    });
  }

  closeEditTaskModal(): void {
    this.editModalOpen = false;
    this.editTaskId = null;
    this.editTask = null;
    this.editLoading = false;
    this.editSaving = false;
  }

  onUpdateTask(request: UpdateTaskRequest): void {
    if (!this.editTaskId) {
      return;
    }

    this.editSaving = true;

    this.dashboardApi.updateTask(this.editTaskId, request).subscribe({
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
        this.cdr.markForCheck();
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

  onMarkComplete(event: { taskId: number; occurrenceDate: string }): void {
    const today = this.dateFormat.toIsoDate();

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

  showError(message: string): void {
    this.errorMessage = message;
    this.successMessage = '';
    this.cdr.markForCheck();
  }

  private showSuccess(message: string): void {
    this.successMessage = message;
    this.errorMessage = '';
    this.cdr.markForCheck();

    setTimeout(() => {
      this.successMessage = '';
      this.cdr.markForCheck();
    }, 2500);
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
