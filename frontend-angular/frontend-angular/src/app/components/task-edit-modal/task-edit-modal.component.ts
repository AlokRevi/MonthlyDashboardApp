import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  CategoryResponse,
  FeelsLikeLabel,
  IntervalUnit,
  RecurrenceType,
  TaskEditScope,
  TaskResponse,
  UpdateTaskRequest,
  WeekOfMonth
} from '../../models/dashboard.models';
import { DateFormatService } from '../../services/date-format.service';

interface SelectOption<T> {
  value: T;
  label: string;
}

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
  @Input() selectedOccurrenceDate: string | null = null;
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
  editScope: TaskEditScope = 'THIS_AND_FOLLOWING';
  editSelectedOccurrenceDate = '';
  editEnergyOverride: FeelsLikeLabel | '' = '';
  editEnjoymentOverride: FeelsLikeLabel | '' = '';
  editPressureOverride: FeelsLikeLabel | '' = '';
  editEffortOverride: FeelsLikeLabel | '' = '';

  readonly energyOptions: SelectOption<FeelsLikeLabel>[] = [
    { value: 'DEATHLY_DRAINING', label: 'Deathly draining' },
    { value: 'TIRING', label: 'Tiring' },
    { value: 'ACTIVATING', label: 'Activating' },
    { value: 'ENERGIZING', label: 'Energizing' }
  ];

  readonly enjoymentOptions: SelectOption<FeelsLikeLabel>[] = [
    { value: 'BORING', label: 'Boring' },
    { value: 'OKAY', label: 'Okay' },
    { value: 'FUN', label: 'Fun' },
    { value: 'BLISSFUL', label: 'Blissful' }
  ];

  readonly pressureOptions: SelectOption<FeelsLikeLabel>[] = [
    { value: 'NO_PRESSURE', label: 'No pressure' },
    { value: 'MILD_FUTURE_STRESS', label: 'Mild future stress' },
    { value: 'URGENT_AND_IMPORTANT', label: 'Urgent and important' },
    { value: 'AMORPHOUS_DREAD', label: 'Amorphous dread' }
  ];

  readonly effortOptions: SelectOption<FeelsLikeLabel>[] = [
    { value: 'EASY', label: 'Easy' },
    { value: 'MEDIUM', label: 'Medium' },
    { value: 'HARD', label: 'Hard' },
    { value: 'VERY_HARD', label: 'Very hard' }
  ];

  constructor(private dateFormat: DateFormatService) {
    this.editTaskStartDate = this.dateFormat.toIsoDate();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['task'] && this.task) {
      this.populateEditForm(this.task);
    }

    if (changes['selectedOccurrenceDate']) {
      this.editSelectedOccurrenceDate = this.selectedOccurrenceDate ?? this.editSelectedOccurrenceDate;
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
    this.editScope = 'THIS_AND_FOLLOWING';
    this.editSelectedOccurrenceDate = this.selectedOccurrenceDate ?? task.startDate;
    this.editEnergyOverride = task.energyOverride ?? '';
    this.editEnjoymentOverride = task.enjoymentOverride ?? '';
    this.editPressureOverride = task.pressureOverride ?? '';
    this.editEffortOverride = task.effortOverride ?? '';
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

    if (!this.editSelectedOccurrenceDate) {
      this.fieldErrors['editSelectedOccurrenceDate'] = 'Choose the occurrence date to apply changes from.';
    }

    if (this.editSelectedOccurrenceDate && this.editSelectedOccurrenceDate < this.editTaskStartDate) {
      this.fieldErrors['editSelectedOccurrenceDate'] = 'Apply-from date cannot be before the task start date.';
    }

    if (this.editTaskEndDate && this.editSelectedOccurrenceDate && this.editTaskEndDate < this.editSelectedOccurrenceDate) {
      this.fieldErrors['editTaskEndDate'] = 'End date cannot be before the apply-from date.';
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
      isActive: true,
      editScope: this.editScope,
      selectedOccurrenceDate: this.editSelectedOccurrenceDate,
      energyOverride: this.editEnergyOverride || null,
      enjoymentOverride: this.editEnjoymentOverride || null,
      pressureOverride: this.editPressureOverride || null,
      effortOverride: this.editEffortOverride || null
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
