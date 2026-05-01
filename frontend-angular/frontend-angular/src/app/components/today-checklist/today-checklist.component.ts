import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

import {
  CategoryRequires,
  ChecklistItem,
  TodayChecklistResponse
} from '../../models/dashboard.models';
import { DateFormatService } from '../../services/date-format.service';

interface ChecklistGroup {
  requires: CategoryRequires;
  label: string;
  items: ChecklistItem[];
}

@Component({
  selector: 'app-today-checklist',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './today-checklist.component.html',
  styleUrl: './today-checklist.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TodayChecklistComponent {
  @Input() checklist: TodayChecklistResponse | null = null;

  @Output() markComplete = new EventEmitter<{
    taskId: number;
    occurrenceDate: string;
  }>();
  @Output() undoComplete = new EventEmitter<{
    taskId: number;
    occurrenceDate: string;
  }>();

  private readonly groupOrder: CategoryRequires[] = ['FOCUS', 'MOVEMENT', 'OUTDOOR'];
  private readonly groupLabels: Record<CategoryRequires, string> = {
    FOCUS: 'Focus',
    MOVEMENT: 'Movement',
    OUTDOOR: 'Outdoor'
  };

  constructor(public dateFormat: DateFormatService) {}

  get overdueItems(): ChecklistItem[] {
    return this.checklist?.items.filter(item => item.status === 'OVERDUE') ?? [];
  }

  get completedTodayItems(): ChecklistItem[] {
    return this.checklist?.items.filter(item => item.status === 'COMPLETED') ?? [];
  }

  get totalCount(): number {
    return this.checklist?.items.length ?? 0;
  }

  get activeCount(): number {
    return this.checklist?.items.filter(item => item.status !== 'COMPLETED').length ?? 0;
  }

  get groupedChecklistItems(): ChecklistGroup[] {
    const items = this.checklist?.items ?? [];

    return this.groupOrder
      .map(requires => ({
        requires,
        label: this.groupLabels[requires],
        items: this.sortItems(items.filter(item => this.getRequires(item) === requires))
      }))
      .filter(group => group.items.length > 0);
  }

  onMarkComplete(item: ChecklistItem): void {
    if (item.status === 'COMPLETED') {
      return;
    }

    this.markComplete.emit({
      taskId: item.taskId,
      occurrenceDate: item.occurrenceDate
    });
  }

  onUndoComplete(item: ChecklistItem): void {
    if (item.status !== 'COMPLETED') {
      return;
    }

    this.undoComplete.emit({
      taskId: item.taskId,
      occurrenceDate: item.occurrenceDate
    });
  }

  getDayNumber(item: ChecklistItem): number {
    return this.dateFormat.getDayNumber(item.occurrenceDate);
  }

  trackByChecklistItem(index: number, item: ChecklistItem): string {
    return `${item.taskId}-${item.occurrenceDate}`;
  }

  trackByGroup(index: number, group: ChecklistGroup): CategoryRequires {
    return group.requires;
  }

  getCategoryColor(item: ChecklistItem): string {
    return item.categoryColor || '#94a3b8';
  }

  private sortItems(items: ChecklistItem[]): ChecklistItem[] {
    return [...items].sort((a, b) => {
      const statusDifference = this.getStatusRank(a) - this.getStatusRank(b);

      if (statusDifference !== 0) {
        return statusDifference;
      }

      const dateDifference = a.occurrenceDate.localeCompare(b.occurrenceDate);

      if (dateDifference !== 0) {
        return dateDifference;
      }

      return a.taskName.localeCompare(b.taskName);
    });
  }

  private getStatusRank(item: ChecklistItem): number {
    if (item.status === 'OVERDUE') {
      return 0;
    }

    if (item.status === 'DUE_TODAY') {
      return 1;
    }

    return 2;
  }

  private getRequires(item: ChecklistItem): CategoryRequires {
    return item.categoryRequires ?? 'FOCUS';
  }
}
