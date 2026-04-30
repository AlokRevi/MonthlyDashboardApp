import {
  IntervalUnit,
  RecurrenceType,
  WeekOfMonth
} from '../../models/dashboard.models';

export const MONTH_OPTIONS = [
  { value: 1, label: 'January' },
  { value: 2, label: 'February' },
  { value: 3, label: 'March' },
  { value: 4, label: 'April' },
  { value: 5, label: 'May' },
  { value: 6, label: 'June' },
  { value: 7, label: 'July' },
  { value: 8, label: 'August' },
  { value: 9, label: 'September' },
  { value: 10, label: 'October' },
  { value: 11, label: 'November' },
  { value: 12, label: 'December' }
];

export const RECURRENCE_OPTIONS: { value: RecurrenceType; label: string }[] = [
  { value: 'FIXED_DATE', label: 'Fixed Date' },
  { value: 'INTERVAL', label: 'Every X Days/Weeks' },
  { value: 'WEEKDAY', label: 'Weekday Pattern' }
];

export const INTERVAL_UNIT_OPTIONS: { value: IntervalUnit; label: string }[] = [
  { value: 'DAYS', label: 'Days' },
  { value: 'WEEKS', label: 'Weeks' }
];

export const WEEKDAY_OPTIONS = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY'
];

export const WEEK_OF_MONTH_OPTIONS: { value: WeekOfMonth; label: string }[] = [
  { value: 'FIRST', label: '1st' },
  { value: 'SECOND', label: '2nd' },
  { value: 'THIRD', label: '3rd' },
  { value: 'FOURTH', label: '4th' },
  { value: 'LAST', label: 'Last' }
];
