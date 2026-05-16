import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

import {
  TimelineCategoryResponse,
  TimelineCellResponse,
  TimelineDashboardResponse,
  TimelineOccurrenceBucketResponse,
  TimelineTaskResponse
} from '../../models/dashboard.models';

@Component({
  selector: 'app-quarter-timeline-grid',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './quarter-timeline-grid.component.html',
  styleUrl: './quarter-timeline-grid.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuarterTimelineGridComponent {
  @Input() dashboard: TimelineDashboardResponse | null = null;
  @Input() categories: TimelineCategoryResponse[] = [];

  get gridTemplateColumns(): string {
    return `240px repeat(${this.dashboard?.cells.length ?? 0}, 62px)`;
  }

  bucketForCell(
    task: TimelineTaskResponse,
    cell: TimelineCellResponse
  ): TimelineOccurrenceBucketResponse | null {
    return task.buckets.find(bucket => bucket.cellKey === cell.key) ?? null;
  }

  hasOccurrences(task: TimelineTaskResponse): boolean {
    return task.buckets.some(bucket => bucket.totalOccurrences > 0);
  }

  cellBucketLabel(cell: TimelineCellResponse): string {
    if (cell.cellType === 'WEEKDAY_BUCKET') {
      return 'Weekdays';
    }

    if (cell.cellType === 'WEEKEND_BUCKET') {
      return 'Weekend';
    }

    return cell.secondaryLabel || cell.label;
  }

  bucketAccessibleLabel(
    bucket: TimelineOccurrenceBucketResponse,
    cell: TimelineCellResponse
  ): string {
    const total = bucket.totalOccurrences;

    if (total === 0) {
      return `No generated occurrences from ${cell.startDate} to ${cell.endDate}`;
    }

    return `${bucket.completedOccurrences} completed of ${total} generated occurrences from ${cell.startDate} to ${cell.endDate}`;
  }
}
