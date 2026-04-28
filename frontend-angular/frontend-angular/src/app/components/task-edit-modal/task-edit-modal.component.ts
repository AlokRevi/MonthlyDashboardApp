import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  CategoryResponse,
  IntervalUnit,
  RecurrenceType,
  TaskResponse,
  UpdateTaskRequest,
  WeekOfMonth
} from '../../models/dashboard.models';
import { DateFormatService } from '../../services/date-format.service';

@Component({
  selector: 'app-task-edit-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './task-edit-modal.component.html',
  styleUrl: './task-edit-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TaskEditModalComponent implements OnChanges {
  @Input() open = false;
  @Input() loading = false;
  @Input() saving = false;
  @Input() task: TaskResponse | null = null;
  @Input() availableCategories: CategoryResponse[] = [];
  @Input() recurrenceOptions: { value: RecurrenceType; label: string }[] = [];
  @Input() intervalUnitOptions: { value: IntervalUnit; label: string }[] = [];
  @Input() weekdayOptions: string[] = [];
  @Input() weekOfMonthOptions: { value: WeekOfMonth; label: string }[] = [];

  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<UpdateTaskRequest>();

  fieldErrors: Record<string, string> = {};

  editTaskCategoryId: number | null = null;
  editTaskName = '';
  editTaskDescription = '';
  editTaskStartDate = '';
  editTaskEndDate: string | null = null;
  editTaskRecurrenceType: RecurrenceType = 'FIXED_DATE';
  editTaskFixedDatesText = '';
  editFallbackToLastDay = true;
  editIntervalValue = 1;
  editIntervalUnit: IntervalUnit = 'WEEKS';
  editWeekday = 'FRIDAY';
  editWeekOfMonth: WeekOfMonth = 'LAST';

  constructor(private dateFormat: DateFormatService) {
    this.editTaskStartDate = this.dateFormat.toIsoDate();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['task'] && this.task) {
      this.populateEditForm(this.task);
    }
  }

  onSave(): void {
    if (!this.validateEditTaskForm()) {
      return;
    }

    this.save.emit(this.buildUpdateTaskRequest());
  }

  private populateEditForm(task: TaskResponse): void {
    this.fieldErrors = {};
    this.editTaskCategoryId = task.categoryId;
    this.editTaskName = task.name;
    this.editTaskDescription = task.description ?? '';
    this.editTaskStartDate = task.startDate || this.dateFormat.toIsoDate();
    this.editTaskEndDate = task.endDate;
    this.editTaskRecurrenceType = task.recurrenceType;
    this.editTaskFixedDatesText = task.rule?.fixedDates?.join(', ') ?? '';
    this.editFallbackToLastDay = task.rule?.fallbackToLastDay ?? true;
    this.editIntervalValue = task.rule?.intervalValue ?? 1;
    this.editIntervalUnit = task.rule?.intervalUnit ?? 'WEEKS';
    this.editWeekday = task.rule?.weekday ?? 'FRIDAY';
    this.editWeekOfMonth = task.rule?.weekOfMonth ?? 'LAST';
  }

  private validateEditTaskForm(): boolean {
    this.fieldErrors = {};

    if (!this.editTaskCategoryId) {
      this.fieldErrors['editTaskCategory'] = 'Please select a category.';
    }

    if (!this.editTaskName.trim()) {
      this.fieldErrors['editTaskName'] = 'Task name is required.';
    }

    if (!this.editTaskStartDate) {
      this.fieldErrors['editTaskStartDate'] = 'Start date is required.';
    }

    if (this.editTaskEndDate && this.editTaskEndDate < this.editTaskStartDate) {
      this.fieldErrors['editTaskEndDate'] = 'End date cannot be before start date.';
    }

    if (this.editTaskRecurrenceType === 'FIXED_DATE') {
      const fixedDates = this.parseEditFixedDates();

      if (fixedDates.length === 0) {
        this.fieldErrors['editFixedDates'] = 'Enter at least one date.';
      }

      if (fixedDates.some(date => date < 1 || date > 31)) {
        this.fieldErrors['editFixedDates'] = 'Each date must be between 1 and 31.';
      }
    }

    if (this.editTaskRecurrenceType === 'INTERVAL' && this.editIntervalValue < 1) {
      this.fieldErrors['editIntervalValue'] = 'Interval must be at least 1.';
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
      endDate: this.editTaskEndDate || null,
      isActive: true
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

  private parseEditFixedDates(): number[] {
    return this.editTaskFixedDatesText
      .split(',')
      .map(value => Number(value.trim()))
      .filter(value => !Number.isNaN(value));
  }
}
