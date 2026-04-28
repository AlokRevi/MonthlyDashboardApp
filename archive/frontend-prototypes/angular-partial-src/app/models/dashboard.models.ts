export interface ScaleBar {
  anchors: number[];
  lastDay: number;
  currentDateLabel: string;
}

export interface DayStripItem {
  date: string;
  dayOfMonth: number;
  weekday: string;
  isToday: boolean;
  isWeekend: boolean;
}

export interface Occurrence {
  occurrenceDate: string;
  dayOfMonth: number;
  completed: boolean;
  completionDate: string | null;
  status: 'UPCOMING' | 'DUE_TODAY' | 'OVERDUE' | 'COMPLETED';
}

export interface DashboardTask {
  taskId: number;
  taskName: string;
  recurrenceType: 'FIXED_DATE' | 'INTERVAL' | 'WEEKDAY';
  occurrences: Occurrence[];
}

export interface DashboardCategory {
  categoryId: number;
  categoryName: string;
  tasks: DashboardTask[];
}

export interface MonthlyDashboardResponse {
  year: number;
  month: number;
  monthLabel: string;
  today: string;
  readOnly: boolean;
  scaleBar: ScaleBar;
  dayStrip: DayStripItem[];
  categories: DashboardCategory[];
}

export interface ChecklistItem {
  taskId: number;
  taskName: string;
  categoryId: number;
  categoryName: string;
  occurrenceDate: string;
  status: 'DUE_TODAY' | 'OVERDUE';
}

export interface TodayChecklistResponse {
  today: string;
  items: ChecklistItem[];
}

export interface CompleteTaskRequest {
  occurrenceDate: string;
  completionDate: string;
}