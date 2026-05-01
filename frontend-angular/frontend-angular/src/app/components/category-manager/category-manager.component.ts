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
  DashboardTask,
  MonthlyDashboardResponse
} from '../../models/dashboard.models';

interface CategoryTaskItem {
  taskId: number;
  taskName: string;
  recurrenceSummary: string;
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
  @Output() editCategory = new EventEmitter<number>();

  expandedCategoryId: number | null = null;
  categoryTaskItemsById: Record<number, CategoryTaskItem[]> = {};

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
        .map(task => this.toCategoryTaskItem(task))
        .sort((a, b) => a.taskName.localeCompare(b.taskName));
    }

    return taskItemsById;
  }

  private toCategoryTaskItem(task: DashboardTask): CategoryTaskItem {
    return {
      taskId: task.taskId,
      taskName: task.taskName,
      recurrenceSummary: task.recurrenceSummary
    };
  }
}
