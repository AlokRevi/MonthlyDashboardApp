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

  @Output() deleteTask = new EventEmitter<number>();
  @Output() editTask = new EventEmitter<{ taskId: number; occurrenceDate: string | null }>();
  @Output() editCategory = new EventEmitter<number>();

  expandedTaskId: number | null = null;
  private readonly todayIso = this.toIsoDate(new Date());

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

  getPastCellClass(day: DayStripItem): string {
    const daysPast = this.getDaysPast(day);

    if (daysPast <= 0) {
      return '';
    }

    if (daysPast <= 7) {
      return 'past-recent';
    }

    if (daysPast <= 21) {
      return 'past-mid';
    }

    return 'past-old';
  }

  isPastDay(day: DayStripItem): boolean {
    return this.getDaysPast(day) > 0;
  }

  isOccurrenceCompleted(occurrence: DashboardOccurrence | null): boolean {
    return occurrence?.completed === true || occurrence?.status === 'COMPLETED';
  }

  onEditTaskClicked(task: DashboardTask): void {
    this.editTask.emit({
      taskId: task.taskId,
      occurrenceDate: this.getDefaultEditOccurrenceDate(task)
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

  private getDaysPast(day: DayStripItem): number {
    if (day.date >= this.todayIso) {
      return 0;
    }

    const dayTime = this.toLocalDate(day.date).getTime();
    const todayTime = this.toLocalDate(this.todayIso).getTime();

    return Math.floor((todayTime - dayTime) / 86_400_000);
  }

  private getDefaultEditOccurrenceDate(task: DashboardTask): string | null {
    const sortedOccurrences = [...task.occurrences]
      .sort((a, b) => a.occurrenceDate.localeCompare(b.occurrenceDate));

    return sortedOccurrences.find(occurrence => occurrence.occurrenceDate >= this.todayIso)?.occurrenceDate
      ?? sortedOccurrences[0]?.occurrenceDate
      ?? null;
  }

  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  private toLocalDate(isoDate: string): Date {
    return new Date(`${isoDate}T00:00:00`);
  }
}
