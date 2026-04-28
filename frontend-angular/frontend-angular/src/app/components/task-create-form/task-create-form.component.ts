import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  CategoryResponse,
  CreateCategoryRequest,
  CreateTaskRequest,
  IntervalUnit,
  RecurrenceType,
  WeekOfMonth
} from '../../models/dashboard.models';
import { DateFormatService } from '../../services/date-format.service';

@Component({
  selector: 'app-task-create-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './task-create-form.component.html',
  styleUrl: './task-create-form.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TaskCreateFormComponent {
  @Input() availableCategories: CategoryResponse[] = [];
  @Input() taskSaving = false;
  @Input() categorySaving = false;
  @Input() recurrenceOptions: { value: RecurrenceType; label: string }[] = [];
  @Input() intervalUnitOptions: { value: IntervalUnit; label: string }[] = [];
  @Input() weekdayOptions: string[] = [];
  @Input() weekOfMonthOptions: { value: WeekOfMonth; label: string }[] = [];

  @Output() createTask = new EventEmitter<CreateTaskRequest>();
  @Output() createCategory = new EventEmitter<CreateCategoryRequest>();
  @Output() validationError = new EventEmitter<string>();

  fieldErrors: Record<string, string> = {};

  newCategoryName = '';
  newCategoryColor = '#2563eb';

  newTaskCategoryId: number | null = null;
  newTaskName = '';
  newTaskDescription = '';
  newTaskStartDate = '';
  newTaskEndDate: string | null = null;
  newTaskRecurrenceType: RecurrenceType = 'FIXED_DATE';
  newTaskFixedDatesText = '';
  fallbackToLastDay = true;
  intervalValue = 1;
  intervalUnit: IntervalUnit = 'WEEKS';
  weekday = 'FRIDAY';
  weekOfMonth: WeekOfMonth = 'LAST';

  constructor(private dateFormat: DateFormatService) {
    this.newTaskStartDate = this.dateFormat.toIsoDate();
  }

  onCreateCategory(): void {
    this.fieldErrors = {};
    const name = this.newCategoryName.trim();

    if (!name) {
      this.fieldErrors['categoryName'] = 'Category name is required.';
      this.validationError.emit('Please fix the highlighted fields.');
      return;
    }

    this.createCategory.emit({
      name,
      color: this.newCategoryColor || '#2563eb'
    });

    this.newCategoryName = '';
    this.newCategoryColor = '#2563eb';
  }

  onCreateTask(): void {
    if (!this.newTaskCategoryId && this.availableCategories.length > 0) {
      this.newTaskCategoryId = this.availableCategories[0].id;
    }

    if (!this.validateTaskForm()) {
      this.validationError.emit('Please fix the highlighted fields.');
      return;
    }

    this.createTask.emit(this.buildCreateTaskRequest());
    this.resetTaskForm();
  }

  private validateTaskForm(): boolean {
    this.fieldErrors = {};

    if (!this.newTaskCategoryId) {
      this.fieldErrors['taskCategory'] = 'Please select a category.';
    }

    if (!this.newTaskName.trim()) {
      this.fieldErrors['taskName'] = 'Task name is required.';
    }

    if (!this.newTaskStartDate) {
      this.fieldErrors['taskStartDate'] = 'Start date is required.';
    }

    if (this.newTaskEndDate && this.newTaskEndDate < this.newTaskStartDate) {
      this.fieldErrors['taskEndDate'] = 'End date cannot be before start date.';
    }

    if (this.newTaskRecurrenceType === 'FIXED_DATE') {
      const fixedDates = this.parseFixedDates();

      if (fixedDates.length === 0) {
        this.fieldErrors['fixedDates'] = 'Enter at least one date.';
      }

      if (fixedDates.some(date => date < 1 || date > 31)) {
        this.fieldErrors['fixedDates'] = 'Each date must be between 1 and 31.';
      }
    }

    if (this.newTaskRecurrenceType === 'INTERVAL' && this.intervalValue < 1) {
      this.fieldErrors['intervalValue'] = 'Interval must be at least 1.';
    }

    if (this.newTaskRecurrenceType === 'WEEKDAY' && !this.weekOfMonth) {
      this.fieldErrors['weekOfMonth'] = 'Choose 1st, 2nd, 3rd, 4th, or Last.';
    }

    return Object.keys(this.fieldErrors).length === 0;
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

  private parseFixedDates(): number[] {
    return this.newTaskFixedDatesText
      .split(',')
      .map(value => Number(value.trim()))
      .filter(value => !Number.isNaN(value));
  }

  private resetTaskForm(): void {
    this.newTaskName = '';
    this.newTaskDescription = '';
    this.newTaskFixedDatesText = '';
    this.fallbackToLastDay = true;
    this.intervalValue = 1;
    this.intervalUnit = 'WEEKS';
    this.weekday = 'FRIDAY';
    this.weekOfMonth = 'LAST';
  }
}
