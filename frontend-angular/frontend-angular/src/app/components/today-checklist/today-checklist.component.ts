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
  // Data received from DashboardPageComponent.
  // This contains today's date and due/overdue checklist items.
  @Input() checklist: TodayChecklistResponse | null = null;

  // Sends selected task occurrence back to the parent component.
  // Parent component will call the backend completion API.
  @Output() markComplete = new EventEmitter<{
    taskId: number;
    occurrenceDate: string;
  }>();

  // Emits the task occurrence that should be marked complete.
  onMarkComplete(item: ChecklistItem): void {
    this.markComplete.emit({
      taskId: item.taskId,
      occurrenceDate: item.occurrenceDate
    });
  }

  // Makes backend enum text easier to read in the UI.
  getStatusLabel(status: string): string {
    if (status === 'DUE_TODAY') {
      return 'Due today';
    }

    if (status === 'OVERDUE') {
      return 'Overdue';
    }

    return status;
  }

  // Helps Angular render list rows efficiently.
  trackByChecklistItem(index: number, item: ChecklistItem): string {
    return `${item.taskId}-${item.occurrenceDate}`;
  }
}
