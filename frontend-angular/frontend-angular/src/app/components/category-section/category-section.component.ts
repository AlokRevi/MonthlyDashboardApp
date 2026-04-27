import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output
} from '@angular/core';

import { CommonModule } from '@angular/common';

import {
  DashboardCategory,
  DashboardOccurrence,
  DashboardTask,
  DayStripItem
} from '../../models/dashboard.models';

@Component({
  selector: 'app-category-section',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './category-section.component.html',
  styleUrl: './category-section.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CategorySectionComponent {
  @Input() categories: DashboardCategory[] = [];
  @Input() dayStrip: DayStripItem[] = [];

  @Output() markComplete = new EventEmitter<{ taskId: number; occurrenceDate: string }>();
  @Output() undoComplete = new EventEmitter<{ taskId: number; occurrenceDate: string }>();
  @Output() deleteTask = new EventEmitter<number>();

  // New: parent opens edit modal.
  @Output() editTask = new EventEmitter<number>();

  expandedTaskId: number | null = null;

  toggleTaskActions(taskId: number): void {
    this.expandedTaskId = this.expandedTaskId === taskId ? null : taskId;
  }

  isTaskExpanded(taskId: number): boolean {
    return this.expandedTaskId === taskId;
  }

  get sortedCategories(): DashboardCategory[] {
    return this.categories
      .filter(category => category.tasks.length > 0)
      .map(category => ({
        ...category,
        tasks: [...category.tasks].sort(
          (a, b) => this.getNextOccurrenceDay(a) - this.getNextOccurrenceDay(b)
        )
      }));
  }

  private getNextOccurrenceDay(task: DashboardTask): number {
    if (task.occurrences.length === 0) {
      return 999;
    }

    return Math.min(...task.occurrences.map(occurrence => occurrence.dayOfMonth));
  }

  getOccurrenceForDay(
    task: DashboardTask,
    day: DayStripItem
  ): DashboardOccurrence | null {
    return task.occurrences.find(
      occurrence => occurrence.occurrenceDate === day.date
    ) ?? null;
  }

  onOccurrenceClicked(task: DashboardTask, occurrence: DashboardOccurrence): void {
    if (occurrence.completed) {
      this.undoComplete.emit({
        taskId: task.taskId,
        occurrenceDate: occurrence.occurrenceDate
      });
      return;
    }

    this.markComplete.emit({
      taskId: task.taskId,
      occurrenceDate: occurrence.occurrenceDate
    });
  }

  trackByCategory(index: number, category: DashboardCategory): number {
    return category.categoryId;
  }

  trackByTask(index: number, task: DashboardTask): number {
    return task.taskId;
  }

  trackByDay(index: number, day: DayStripItem): string {
    return day.date;
  }
}
