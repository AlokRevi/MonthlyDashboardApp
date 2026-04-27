// -------------------------------
// Dashboard API response models
// -------------------------------

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

export interface DashboardCategory {
  categoryId: number;
  categoryName: string;
  tasks: DashboardTask[];
}

export interface DashboardTask {
  taskId: number;
  taskName: string;
  recurrenceType: string;
  occurrences: DashboardOccurrence[];
}

export interface DashboardOccurrence {
  occurrenceDate: string;
  dayOfMonth: number;
  completed: boolean;
  completionDate: string | null;
  status: 'UPCOMING' | 'DUE_TODAY' | 'OVERDUE' | 'COMPLETED';
}

// -------------------------------
// Today checklist models
// -------------------------------

export interface TodayChecklistResponse {
  today: string;
  items: ChecklistItem[];
}

export interface ChecklistItem {
  taskId: number;
  taskName: string;
  categoryId: number;
  categoryName: string;
  occurrenceDate: string;
  status: 'DUE_TODAY' | 'OVERDUE';
}

// -------------------------------
// Completion request
// -------------------------------

export interface CompleteTaskRequest {
  occurrenceDate: string;
  completionDate: string;
}

// -------------------------------
// Category models
// -------------------------------

export interface CreateCategoryRequest {
  name: string;
  color: string;
}

export interface CategoryResponse {
  id: number;
  name: string;
  color: string;
  taskCount: number;
  createdAt: string;
  updatedAt?: string;
}

// -------------------------------
// Task models
// -------------------------------

export type RecurrenceType = 'FIXED_DATE' | 'INTERVAL' | 'WEEKDAY';
export type IntervalUnit = 'DAYS' | 'WEEKS';

export interface TaskRuleRequest {
  fixedDates?: number[];
  fallbackToLastDay?: boolean;
  intervalValue?: number;
  intervalUnit?: IntervalUnit;
  weekday?: string;
  weekOfMonth?: number;
}

export interface CreateTaskRequest {
  categoryId: number;
  name: string;
  description: string;
  recurrenceType: RecurrenceType;
  startDate: string;
  endDate: string | null;
  rule: TaskRuleRequest;
}

export interface UpdateTaskRequest {
  categoryId: number;
  name: string;
  description: string;
  recurrenceType: RecurrenceType;
  startDate: string;
  endDate: string | null;
  rule: TaskRuleRequest;
}

export interface TaskResponse {
  id: number;
  categoryId: number;
  categoryName: string;
  name: string;
  description: string;
  recurrenceType: RecurrenceType;
  startDate: string;
  endDate: string | null;
  isActive: boolean;
  rule?: TaskRuleRequest;
}
