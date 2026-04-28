import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { TodayChecklistResponse, ChecklistItem } from '../../models/dashboard.models';

interface ChecklistItemViewModel extends ChecklistItem {
  formattedOccurrenceDate: string;
  formattedStatus: string;
}

@Component({
  selector: 'app-today-checklist',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './today-checklist.component.html',
  styleUrl: './today-checklist.component.css'
})
export class TodayChecklistComponent implements OnChanges {
  @Input({ required: true }) checklist!: TodayChecklistResponse;
  @Output() markComplete = new EventEmitter<{ taskId: number; occurrenceDate: string }>();

  formattedToday = '';
  viewItems: ChecklistItemViewModel[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['checklist'] && this.checklist) {
      this.formattedToday = this.formatDateNice(this.checklist.today);
      this.viewItems = this.checklist.items.map(item => ({
        ...item,
        formattedOccurrenceDate: this.formatDateNice(item.occurrenceDate),
        formattedStatus: this.formatStatus(item.status)
      }));
    }
  }

  trackByChecklistItem(_: number, item: ChecklistItemViewModel): string {
    return `${item.taskId}-${item.occurrenceDate}`;
  }

  onMarkComplete(item: ChecklistItem): void {
    this.markComplete.emit({
      taskId: item.taskId,
      occurrenceDate: item.occurrenceDate
    });
  }

  private formatStatus(status: string): string {
    if (status === 'DUE_TODAY') return 'Due Today';
    if (status === 'OVERDUE') return 'Overdue';
    return status;
  }

  private formatDateNice(dateStr: string): string {
    const date = new Date(dateStr + 'T00:00:00');
    const month = date.toLocaleString('en-US', { month: 'short' });
    return `${month} ${date.getDate()}, ${date.getFullYear()}`;
  }
}
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'app-timeline-strip',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './timeline-strip.component.html',
  styleUrl: './timeline-strip.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})