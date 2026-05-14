import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  CategoryResponse,
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
export class TaskCreateFormComponent implements OnChanges {
  @Input() availableCategories: CategoryResponse[] = [];
  @Input() taskSaving = false;
  @Input() categorySaving = false;
  @Input() taskCreateSuccessCount = 0;
  @Input() preselectedCategoryId: number | null = null;
  @Input() showHeader = true;
  @Input() recurrenceOptions: { value: RecurrenceType; label: string }[] = [];
  @Input() intervalUnitOptions: { value: IntervalUnit; label: string }[] = [];
  @Input() weekdayOptions: string[] = [];
  @Input() weekOfMonthOptions: { value: WeekOfMonth; label: string }[] = [];

  @Output() createTask = new EventEmitter<CreateTaskRequest>();
  @Output() openCategoryDialog = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
  @Output() validationError = new EventEmitter<string>();

  fieldErrors: Record<string, string> = {};
  private readonly fixedDatesInvalidMessage = 'Use only numbers from 1-31, separated by commas.';

  newTaskCategoryId: number | null = null;
  newTaskName = '';
  newTaskDescription = '';
  newTaskStartDate = '';
  newTaskEndDate: string | null = null;
  private autoDefaultEndDate: string | null = null;
  newTaskRecurrenceType: RecurrenceType = 'FIXED_DATE';
  newTaskFixedDatesText = '';
  fallbackToLastDay = true;
  intervalValue = 1;
  intervalUnit: IntervalUnit = 'WEEKS';
  weekday = 'FRIDAY';
  weekOfMonth: WeekOfMonth = 'LAST';

  constructor(private dateFormat: DateFormatService) {
    this.newTaskStartDate = this.dateFormat.toIsoDate();
    this.setDefaultEndDateFromStartDate();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['availableCategories']) {
      this.initializeSelectedCategory();
    }

    if (changes['preselectedCategoryId']) {
      this.applyPreselectedCategory();
    }

    if (
      changes['taskCreateSuccessCount']
      && !changes['taskCreateSuccessCount'].firstChange
    ) {
      this.resetTaskForm();
    }
  }

  onCreateTask(): void {
    this.initializeSelectedCategory();

    if (!this.validateTaskForm()) {
      this.validationError.emit('Please fix the highlighted fields.');
      return;
    }

    this.createTask.emit(this.buildCreateTaskRequest());
  }

  onStartDateChange(startDate: string): void {
    const previousAutoDefaultEndDate = this.autoDefaultEndDate;

    this.newTaskStartDate = startDate;

    if (
      !this.newTaskEndDate
      || this.newTaskEndDate === previousAutoDefaultEndDate
    ) {
      this.setDefaultEndDateFromStartDate();
    } else {
      this.autoDefaultEndDate = this.getOneYearAfterIsoDate(startDate);
    }

    this.clearFieldError('taskStartDate');
    this.validateEndDateField();
  }

  onEndDateChange(endDate: string | null): void {
    this.newTaskEndDate = endDate || null;
    this.validateEndDateField();
  }

  onFixedDatesBeforeInput(event: Event): void {
    const inputEvent = event as InputEvent;

    if (!inputEvent.data || /^[0-9,\s]+$/.test(inputEvent.data)) {
      return;
    }

    event.preventDefault();
    this.fieldErrors['fixedDates'] = this.fixedDatesInvalidMessage;
  }

  onFixedDatesTextChange(value: string): void {
    this.newTaskFixedDatesText = value;
    this.validateFixedDatesField();
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
      this.validateFixedDatesField();
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
      .map(value => Number(value.trim()));
  }

  private resetTaskForm(): void {
    this.fieldErrors = {};
    this.newTaskName = '';
    this.newTaskDescription = '';
    this.newTaskFixedDatesText = '';
    this.fallbackToLastDay = true;
    this.intervalValue = 1;
    this.intervalUnit = 'WEEKS';
    this.weekday = 'FRIDAY';
    this.weekOfMonth = 'LAST';
    this.newTaskStartDate = this.dateFormat.toIsoDate();
    this.setDefaultEndDateFromStartDate();
    this.initializeSelectedCategory();
  }

  private initializeSelectedCategory(): void {
    if (this.applyPreselectedCategory()) {
      return;
    }

    if (
      this.availableCategories.length > 0
      && !this.availableCategories.some(category => category.id === this.newTaskCategoryId)
    ) {
      this.newTaskCategoryId = this.availableCategories[0].id;
    }
  }

  private applyPreselectedCategory(): boolean {
    if (
      this.preselectedCategoryId
      && this.availableCategories.some(category => category.id === this.preselectedCategoryId)
    ) {
      this.newTaskCategoryId = this.preselectedCategoryId;
      return true;
    }

    return false;
  }

  private validateFixedDatesField(): void {
    const validationMessage = this.getFixedDatesValidationMessage();

    if (validationMessage) {
      this.fieldErrors['fixedDates'] = validationMessage;
      return;
    }

    this.clearFieldError('fixedDates');
  }

  private getFixedDatesValidationMessage(): string | null {
    const value = this.newTaskFixedDatesText.trim();

    if (!value) {
      return 'Enter at least one date.';
    }

    if (!/^[0-9,\s]+$/.test(value)) {
      return this.fixedDatesInvalidMessage;
    }

    const tokens = value.split(',').map(token => token.trim());

    if (tokens.some(token => token === '' || !/^\d+$/.test(token))) {
      return this.fixedDatesInvalidMessage;
    }

    const fixedDates = tokens.map(token => Number(token));

    if (fixedDates.some(date => date < 1 || date > 31)) {
      return this.fixedDatesInvalidMessage;
    }

    return null;
  }

  private validateEndDateField(): void {
    if (!this.newTaskStartDate || !this.newTaskEndDate) {
      this.clearFieldError('taskEndDate');
      return;
    }

    if (this.newTaskEndDate < this.newTaskStartDate) {
      this.fieldErrors['taskEndDate'] = 'End date cannot be before start date.';
      return;
    }

    this.clearFieldError('taskEndDate');
  }

  private clearFieldError(fieldName: string): void {
    if (!this.fieldErrors[fieldName]) {
      return;
    }

    const { [fieldName]: _removed, ...remainingErrors } = this.fieldErrors;
    this.fieldErrors = remainingErrors;
  }

  private setDefaultEndDateFromStartDate(): void {
    this.autoDefaultEndDate = this.getOneYearAfterIsoDate(this.newTaskStartDate);
    this.newTaskEndDate = this.autoDefaultEndDate;
  }

  private getOneYearAfterIsoDate(isoDate: string): string | null {
    if (!isoDate) {
      return null;
    }

    const [year, month, day] = isoDate.split('-').map(value => Number(value));

    if (!year || !month || !day) {
      return null;
    }

    const nextYearDate = new Date(year + 1, month - 1, day);
    const nextYear = nextYearDate.getFullYear();
    const nextMonth = String(nextYearDate.getMonth() + 1).padStart(2, '0');
    const nextDay = String(nextYearDate.getDate()).padStart(2, '0');

    return `${nextYear}-${nextMonth}-${nextDay}`;
  }
}
