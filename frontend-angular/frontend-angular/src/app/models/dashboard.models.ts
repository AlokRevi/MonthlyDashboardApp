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
  categoryColor: string;
  tasks: DashboardTask[];
}

export interface DashboardTask {
  taskId: number;
  taskName: string;
  recurrenceType: RecurrenceType;
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
  categoryColor: string;
  categoryRequires: CategoryRequires;
  categoryFeelsLike: FeelsLikeLabel[];
  occurrenceDate: string;
  status: 'DUE_TODAY' | 'OVERDUE' | 'COMPLETED';
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
  requires?: CategoryRequires;
  feelsLike?: FeelsLikeLabel[];
}

export interface CategoryResponse {
  id: number;
  name: string;
  color: string;
  requires: CategoryRequires;
  feelsLike: FeelsLikeLabel[];
  taskCount: number;
  createdAt: string;
  updatedAt?: string;
}

export interface UpdateCategoryRequest {
  name: string;
  color: string;
  requires?: CategoryRequires;
  feelsLike?: FeelsLikeLabel[];
}

export type CategoryRequires = 'FOCUS' | 'MOVEMENT' | 'OUTDOOR';
export type FeelsLikeLabel =
  | 'QUICK_WIN'
  | 'DEEP_WORK'
  | 'ROUTINE'
  | 'ENERGY_BOOST'
  | 'RESET';

// -------------------------------
// Task models
// -------------------------------

export type RecurrenceType = 'FIXED_DATE' | 'INTERVAL' | 'WEEKDAY';
export type IntervalUnit = 'DAYS' | 'WEEKS';

// API spec uses FIRST/SECOND/THIRD/FOURTH/LAST.
export type WeekOfMonth = 'FIRST' | 'SECOND' | 'THIRD' | 'FOURTH' | 'LAST';

export interface TaskRuleRequest {
  fixedDates?: number[];
  fallbackToLastDay?: boolean;

  intervalValue?: number;
  intervalUnit?: IntervalUnit;

  weekday?: string;
  weekOfMonth?: WeekOfMonth;
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
  isActive?: boolean;
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
