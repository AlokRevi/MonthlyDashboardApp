import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output
} from '@angular/core';
import { CommonModule } from '@angular/common';

import {
  CategoryResponse,
  MonthlyDashboardResponse
} from '../../models/dashboard.models';
import { DateFormatService } from '../../services/date-format.service';

interface CategoryTaskItem {
  taskName: string;
  occurrenceDate: string;
}

@Component({
  selector: 'app-category-manager',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './category-manager.component.html',
  styleUrl: './category-manager.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CategoryManagerComponent implements OnChanges {
  @Input() categories: CategoryResponse[] = [];
  @Input() dashboard: MonthlyDashboardResponse | null = null;

  @Output() deleteCategory = new EventEmitter<number>();

  expandedCategoryId: number | null = null;
  categoryTaskItemsById: Record<number, CategoryTaskItem[]> = {};

  constructor(public dateFormat: DateFormatService) {}

  ngOnChanges(): void {
    this.categoryTaskItemsById = this.buildCategoryTaskItemsById();
  }

  toggleCategory(categoryId: number): void {
    this.expandedCategoryId = this.expandedCategoryId === categoryId ? null : categoryId;
  }

  isCategoryExpanded(categoryId: number): boolean {
    return this.expandedCategoryId === categoryId;
  }

  private buildCategoryTaskItemsById(): Record<number, CategoryTaskItem[]> {
    const taskItemsById: Record<number, CategoryTaskItem[]> = {};

    for (const category of this.categories) {
      taskItemsById[category.id] = [];
    }

    for (const category of this.dashboard?.categories ?? []) {
      taskItemsById[category.categoryId] = category.tasks
        .flatMap(task =>
          task.occurrences.map(occurrence => ({
            taskName: task.taskName,
            occurrenceDate: occurrence.occurrenceDate
          }))
        )
        .sort((a, b) => a.occurrenceDate.localeCompare(b.occurrenceDate));
    }

    return taskItemsById;
  }
}
