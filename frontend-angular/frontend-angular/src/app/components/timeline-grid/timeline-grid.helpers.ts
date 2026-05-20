import {
  TimelineCellResponse,
  TimelineOccurrenceBucketResponse,
  TimelineTaskResponse
} from '../../models/dashboard.models';

export function bucketForCell(
  task: TimelineTaskResponse,
  cell: TimelineCellResponse
): TimelineOccurrenceBucketResponse | null {
  return task.buckets.find(bucket => bucket.cellKey === cell.key) ?? null;
}

export function hasOccurrences(task: TimelineTaskResponse): boolean {
  return task.buckets.some(bucket => bucket.totalOccurrences > 0);
}

export function dateRangeLabel(cell: TimelineCellResponse): string {
  return cell.startDate === cell.endDate
    ? cell.startDate
    : `${cell.startDate} to ${cell.endDate}`;
}

export function shortDateRangeLabel(cell: TimelineCellResponse): string {
  return `${shortDate(cell.startDate)}-${shortDate(cell.endDate)}`;
}

export function generatedCountLabel(bucket: TimelineOccurrenceBucketResponse): string {
  const total = bucket.totalOccurrences;

  if (total === 1) {
    return '1 generated';
  }

  return `${total} generated`;
}

export function bucketAccessibleLabel(
  bucket: TimelineOccurrenceBucketResponse,
  cell: TimelineCellResponse
): string {
  const total = bucket.totalOccurrences;

  if (total === 0) {
    return `No generated occurrences from ${cell.startDate} to ${cell.endDate}`;
  }

  return `${bucket.completedOccurrences} completed of ${total} generated occurrences from ${cell.startDate} to ${cell.endDate}`;
}

function shortDate(date: string): string {
  const [, month, day] = date.split('-');

  return `${Number(month)}/${Number(day)}`;
}
