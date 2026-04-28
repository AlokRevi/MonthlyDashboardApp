import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

import {
  CategoryResponse,
  MonthlyDashboardResponse
} from '../../models/dashboard.models';
import { DateFormatService } from '../../services/date-format.service';

@Component({
  selector: 'app-category-manager',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './category-manager.component.html',
  styleUrl: './category-manager.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CategoryManagerComponent {
  @Input() categories: CategoryResponse[] = [];
  @Input() dashboard: MonthlyDashboardResponse | null = null;

  @Output() deleteCategory = new EventEmitter<number>();

  expandedCategoryId: number | null = null;

  constructor(public dateFormat: DateFormatService) {}

  toggleCategory(categoryId: number): void {
    this.expandedCategoryId = this.expandedCategoryId === categoryId ? null : categoryId;
  }

  isCategoryExpanded(categoryId: number): boolean {
    return this.expandedCategoryId === categoryId;
  }

  getCategoryTaskItems(categoryId: number): {
    taskName: string;
    occurrenceDate: string;
  }[] {
    const category = this.dashboard?.categories.find(
      item => item.categoryId === categoryId
    );

    if (!category) {
      return [];
    }

    return category.tasks
      .flatMap(task =>
        task.occurrences.map(occurrence => ({
          taskName: task.taskName,
          occurrenceDate: occurrence.occurrenceDate
        }))
      )
      .sort((a, b) => a.occurrenceDate.localeCompare(b.occurrenceDate));
  }

}
