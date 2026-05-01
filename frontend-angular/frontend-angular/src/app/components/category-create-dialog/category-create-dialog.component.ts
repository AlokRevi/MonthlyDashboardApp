import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  CategoryResponse,
  CategoryRequires,
  CreateCategoryRequest,
  FeelsLikeLabel,
  UpdateCategoryRequest
} from '../../models/dashboard.models';

interface SelectOption<T> {
  value: T;
  label: string;
}

@Component({
  selector: 'app-category-create-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './category-create-dialog.component.html',
  styleUrl: './category-create-dialog.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CategoryCreateDialogComponent implements OnChanges {
  @Input() open = false;
  @Input() saving = false;
  @Input() category: CategoryResponse | null = null;

  @Output() close = new EventEmitter<void>();
  @Output() createCategory = new EventEmitter<CreateCategoryRequest>();
  @Output() updateCategory = new EventEmitter<{ categoryId: number; request: UpdateCategoryRequest }>();

  fieldErrors: Record<string, string> = {};

  name = '';
  color = '#2563eb';
  requires: CategoryRequires = 'FOCUS';
  energy: FeelsLikeLabel | '' = '';
  enjoyment: FeelsLikeLabel | '' = '';
  pressure: FeelsLikeLabel | '' = '';
  effort: FeelsLikeLabel | '' = '';

  readonly requiresOptions: SelectOption<CategoryRequires>[] = [
    { value: 'FOCUS', label: 'Focus' },
    { value: 'MOVEMENT', label: 'Movement' },
    { value: 'OUTDOOR', label: 'Outdoor' }
  ];

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

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] && !this.open) {
      this.reset();
    }

    if ((changes['open'] || changes['category']) && this.open) {
      this.populateForMode();
    }
  }

  onClose(): void {
    if (this.saving) {
      return;
    }

    this.close.emit();
  }

  onSave(): void {
    if (!this.validate()) {
      return;
    }

    const request = {
      name: this.name.trim(),
      color: this.color || '#2563eb',
      requires: this.requires,
      feelsLike: [
        this.energy,
        this.enjoyment,
        this.pressure,
        this.effort
      ] as FeelsLikeLabel[]
    };

    if (this.category) {
      this.updateCategory.emit({
        categoryId: this.category.id,
        request
      });
      return;
    }

    this.createCategory.emit(request);
  }

  get dialogTitle(): string {
    return this.category ? 'Edit Category' : 'Create Category';
  }

  get primaryActionLabel(): string {
    if (this.saving) {
      return 'Saving...';
    }

    return this.category ? 'Save Category' : 'Create Category';
  }

  reset(): void {
    this.fieldErrors = {};
    this.name = '';
    this.color = '#2563eb';
    this.requires = 'FOCUS';
    this.energy = '';
    this.enjoyment = '';
    this.pressure = '';
    this.effort = '';
  }

  private populateForMode(): void {
    if (!this.category) {
      this.reset();
      return;
    }

    this.fieldErrors = {};
    this.name = this.category.name;
    this.color = this.category.color || '#2563eb';
    this.requires = this.category.requires ?? 'FOCUS';
    this.energy = this.findProfileValue(this.energyOptions);
    this.enjoyment = this.findProfileValue(this.enjoymentOptions);
    this.pressure = this.findProfileValue(this.pressureOptions);
    this.effort = this.findProfileValue(this.effortOptions);
  }

  private findProfileValue(options: SelectOption<FeelsLikeLabel>[]): FeelsLikeLabel | '' {
    return options.find(option => this.category?.feelsLike?.includes(option.value))?.value ?? '';
  }

  private validate(): boolean {
    this.fieldErrors = {};

    if (!this.name.trim()) {
      this.fieldErrors['name'] = 'Category name is required.';
    }

    if (!this.requires) {
      this.fieldErrors['requires'] = 'Choose what this category requires.';
    }

    if (!this.energy) {
      this.fieldErrors['energy'] = 'Choose an energy profile.';
    }

    if (!this.enjoyment) {
      this.fieldErrors['enjoyment'] = 'Choose an enjoyment profile.';
    }

    if (!this.pressure) {
      this.fieldErrors['pressure'] = 'Choose a pressure profile.';
    }

    if (!this.effort) {
      this.fieldErrors['effort'] = 'Choose an effort profile.';
    }

    return Object.keys(this.fieldErrors).length === 0;
  }
}
