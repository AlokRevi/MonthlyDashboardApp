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
  hasOccurrences
} from '../timeline-grid/timeline-grid.helpers';

@Component({
  selector: 'app-quadrimester-timeline-grid',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './quadrimester-timeline-grid.component.html',
  styleUrl: './quadrimester-timeline-grid.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuadrimesterTimelineGridComponent {
  @Input() dashboard: TimelineDashboardResponse | null = null;
  @Input() categories: TimelineCategoryResponse[] = [];

  get gridTemplateColumns(): string {
    return `240px repeat(${this.dashboard?.cells.length ?? 0}, 62px)`;
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

  cellBucketLabel(cell: TimelineCellResponse): string {
    if (cell.cellType === 'WEEKDAY_BUCKET') {
      return 'Weekdays';
    }

    if (cell.cellType === 'WEEKEND_BUCKET') {
      return 'Weekend';
    }

    return cell.secondaryLabel || cell.label;
  }

  cellBucketRangeLabel(cell: TimelineCellResponse): string {
    if (cell.cellType === 'WEEKDAY_BUCKET') {
      return 'Mon-Fri';
    }

    if (cell.cellType === 'WEEKEND_BUCKET') {
      return 'Sat-Sun';
    }

    return dateRangeLabel(cell);
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
