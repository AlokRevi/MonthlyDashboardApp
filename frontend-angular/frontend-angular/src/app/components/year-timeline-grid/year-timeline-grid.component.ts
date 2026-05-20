import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

import {
  TimelineCategoryResponse,
  TimelineCellResponse,
  TimelineDashboardResponse,
  TimelineOccurrenceBucketResponse,
  TimelineTaskResponse
} from '../../models/dashboard.models';
import {
  bucketAccessibleLabel,
  bucketForCell,
  dateRangeLabel,
  hasOccurrences,
  shortDateRangeLabel
} from '../timeline-grid/timeline-grid.helpers';

@Component({
  selector: 'app-year-timeline-grid',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './year-timeline-grid.component.html',
  styleUrl: './year-timeline-grid.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class YearTimelineGridComponent {
  @Input() dashboard: TimelineDashboardResponse | null = null;
  @Input() categories: TimelineCategoryResponse[] = [];

  get gridTemplateColumns(): string {
    return `240px repeat(${this.dashboard?.cells.length ?? 0}, 60px)`;
  }

  bucketForCell(
    task: TimelineTaskResponse,
    cell: TimelineCellResponse
  ): TimelineOccurrenceBucketResponse | null {
    return bucketForCell(task, cell);
  }

  hasOccurrences(task: TimelineTaskResponse): boolean {
    return hasOccurrences(task);
  }

  weekRangeLabel(cell: TimelineCellResponse): string {
    return dateRangeLabel(cell);
  }

  weekShortRangeLabel(cell: TimelineCellResponse): string {
    return shortDateRangeLabel(cell);
  }

  weekPositionLabel(cell: TimelineCellResponse): string {
    return `${cell.segmentIndex} of ${this.dashboard?.cells.length ?? 0}`;
  }

  bucketDetailLabel(bucket: TimelineOccurrenceBucketResponse): string {
    const total = bucket.totalOccurrences;

    return `${bucket.completedOccurrences} done of ${total}`;
  }

  bucketAccessibleLabel(
    bucket: TimelineOccurrenceBucketResponse,
    cell: TimelineCellResponse
  ): string {
    return bucketAccessibleLabel(bucket, cell);
  }
}
