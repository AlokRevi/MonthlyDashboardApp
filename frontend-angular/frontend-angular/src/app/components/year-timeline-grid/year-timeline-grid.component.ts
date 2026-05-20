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
    return `240px repeat(${this.dashboard?.cells.length ?? 0}, 58px)`;
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

  weekRangeLabel(cell: TimelineCellResponse): string {
    return cell.startDate === cell.endDate
      ? cell.startDate
      : `${cell.startDate} to ${cell.endDate}`;
  }

  weekShortRangeLabel(cell: TimelineCellResponse): string {
    return `${this.shortDate(cell.startDate)}-${this.shortDate(cell.endDate)}`;
  }

  bucketDetailLabel(bucket: TimelineOccurrenceBucketResponse): string {
    const total = bucket.totalOccurrences;

    if (total === 1) {
      return '1 generated';
    }

    return `${total} generated`;
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

  private shortDate(date: string): string {
    const [, month, day] = date.split('-');

    return `${Number(month)}/${Number(day)}`;
  }
}
