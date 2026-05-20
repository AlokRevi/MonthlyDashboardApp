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
  generatedCountLabel,
  hasOccurrences,
  shortDateRangeLabel
} from '../timeline-grid/timeline-grid.helpers';

@Component({
  selector: 'app-halfyear-timeline-grid',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './halfyear-timeline-grid.component.html',
  styleUrl: './halfyear-timeline-grid.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HalfyearTimelineGridComponent {
  @Input() dashboard: TimelineDashboardResponse | null = null;
  @Input() categories: TimelineCategoryResponse[] = [];

  get gridTemplateColumns(): string {
    return `240px repeat(${this.dashboard?.cells.length ?? 0}, 58px)`;
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

  bucketDetailLabel(bucket: TimelineOccurrenceBucketResponse): string {
    return generatedCountLabel(bucket);
  }

  bucketAccessibleLabel(
    bucket: TimelineOccurrenceBucketResponse,
    cell: TimelineCellResponse
  ): string {
    return bucketAccessibleLabel(bucket, cell);
  }
}
