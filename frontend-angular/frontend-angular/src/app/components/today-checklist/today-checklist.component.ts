import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

import {
  ChecklistItem,
  TodayChecklistResponse
} from '../../models/dashboard.models';

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

  // Mobile/touch state for showing overdue list.
  overdueExpanded = false;

  get dueTodayItems(): ChecklistItem[] {
    return this.checklist?.items.filter(item => item.status === 'DUE_TODAY') ?? [];
  }

  get overdueItems(): ChecklistItem[] {
    return this.checklist?.items.filter(item => item.status === 'OVERDUE') ?? [];
  }

  get totalCount(): number {
    return this.checklist?.items.length ?? 0;
  }

  toggleOverdueList(): void {
    this.overdueExpanded = !this.overdueExpanded;
  }

  onMarkComplete(item: ChecklistItem): void {
    this.markComplete.emit({
      taskId: item.taskId,
      occurrenceDate: item.occurrenceDate
    });
  }

  trackByChecklistItem(index: number, item: ChecklistItem): string {
    return `${item.taskId}-${item.occurrenceDate}`;
  }
}
