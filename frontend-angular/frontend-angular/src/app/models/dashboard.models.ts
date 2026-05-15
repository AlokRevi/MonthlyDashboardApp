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

export type TimelineView = 'MONTH' | 'QUARTER' | 'QUADRIMESTER' | 'HALF_YEAR' | 'YEAR';
export type TimelineCellType = 'DAY' | 'WEEKDAY_BUCKET' | 'WEEKEND_BUCKET' | 'WEEK';
export type StartOfWeek = 'SUNDAY' | 'MONDAY';
export type ScaleNumbering = 'SEGMENT' | 'CONTINUED';

export interface ViewSettings {
  view: TimelineView;
  startOfWeek: StartOfWeek;
  scaleNumbering: ScaleNumbering;
  calendarYearBound: boolean;
}

export interface TimelineDashboardResponse {
  view: TimelineView;
  year: number;
  month: number;
  startDate: string;
  endDate: string;
  label: string;
  today: string;
  readOnly: boolean;
  settings: TimelineSettingsResponse;
  scaleBar: TimelineScaleBarResponse;
  cells: TimelineCellResponse[];
  categories: TimelineCategoryResponse[];
}

export interface TimelineSettingsResponse {
  view: TimelineView;
  startOfWeek: StartOfWeek;
  scaleNumbering: ScaleNumbering;
  calendarYearBound: boolean;
}

export interface TimelineScaleBarResponse {
  anchorCellKeys: string[];
  currentDateLabel: string;
}

export interface TimelineCellResponse {
  key: string;
  startDate: string;
  endDate: string;
  label: string;
  secondaryLabel: string;
  cellType: TimelineCellType;
  segmentIndex: number;
  continuedIndex: number;
  isToday: boolean;
  isWeekend: boolean;
  isCurrentPeriod: boolean;
}

export interface TimelineCategoryResponse {
  categoryId: number;
  categoryName: string;
  categoryColor: string;
  tasks: TimelineTaskResponse[];
}

export interface TimelineTaskResponse {
  taskId: number;
  taskName: string;
  recurrenceType: RecurrenceType;
  recurrenceSummary: string;
  buckets: TimelineOccurrenceBucketResponse[];
}

export interface TimelineOccurrenceBucketResponse {
  cellKey: string;
  totalOccurrences: number;
  completedOccurrences: number;
  completionLabel: string;
  collapsedLabel: string;
  occurrences: TimelineOccurrenceResponse[];
}

export interface TimelineOccurrenceResponse {
  occurrenceDate: string;
  completed: boolean;
  completionDate: string | null;
  status: 'UPCOMING' | 'DUE_TODAY' | 'OVERDUE' | 'COMPLETED';
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
  recurrenceSummary: string;
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
  categoryColor?: string;
  categoryRequires?: CategoryRequires;
  categoryFeelsLike?: FeelsLikeLabel[];
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
  | 'RESET'
  | 'DEATHLY_DRAINING'
  | 'TIRING'
  | 'ACTIVATING'
  | 'ENERGIZING'
  | 'BORING'
  | 'OKAY'
  | 'FUN'
  | 'BLISSFUL'
  | 'NO_PRESSURE'
  | 'MILD_FUTURE_STRESS'
  | 'URGENT_AND_IMPORTANT'
  | 'AMORPHOUS_DREAD'
  | 'EASY'
  | 'MEDIUM'
  | 'HARD'
  | 'VERY_HARD';

// -------------------------------
// Task models
// -------------------------------

export type RecurrenceType = 'FIXED_DATE' | 'INTERVAL' | 'WEEKDAY';
export type IntervalUnit = 'DAYS' | 'WEEKS' | 'MONTHS';
export type TaskEditScope = 'THIS_AND_FOLLOWING' | 'ALL_FUTURE';

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
  editScope?: TaskEditScope;
  selectedOccurrenceDate?: string;
  energyOverride?: FeelsLikeLabel | null;
  enjoymentOverride?: FeelsLikeLabel | null;
  pressureOverride?: FeelsLikeLabel | null;
  effortOverride?: FeelsLikeLabel | null;
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
  energyOverride?: FeelsLikeLabel | null;
  enjoymentOverride?: FeelsLikeLabel | null;
  pressureOverride?: FeelsLikeLabel | null;
  effortOverride?: FeelsLikeLabel | null;
  rule?: TaskRuleRequest;
}

// -------------------------------
// Setup export models
// -------------------------------

export interface SetupSnapshotResponse {
  exportedAt: string;
  version: 'v2-setup-snapshot';
  categories: SetupSnapshotCategory[];
  tasks: SetupSnapshotTask[];
}

export interface SetupSnapshotCategory {
  id: number;
  name: string;
  color: string;
  requires: CategoryRequires;
  feelsLike: FeelsLikeLabel[];
  taskCount: number;
}

export interface SetupSnapshotTask {
  id: number;
  categoryId: number;
  categoryName: string;
  name: string;
  description: string;
  recurrenceType: RecurrenceType;
  startDate: string;
  endDate: string | null;
  isActive: boolean;
  energyOverride?: FeelsLikeLabel | null;
  enjoymentOverride?: FeelsLikeLabel | null;
  pressureOverride?: FeelsLikeLabel | null;
  effortOverride?: FeelsLikeLabel | null;
  rule: SetupSnapshotRule;
}

export interface SetupSnapshotRule {
  fixedDates: number[];
  fallbackToLastDay: boolean | null;
  intervalValue: number | null;
  intervalUnit: IntervalUnit | null;
  weekday: string | null;
  weekOfMonth: WeekOfMonth | null;
}

export interface SetupImportPreviewResponse {
  valid: boolean;
  version: string | null;
  categoryCount: number;
  taskCount: number;
  activeTaskCount: number;
  inactiveTaskCount: number;
  warnings: string[];
  errors: string[];
}

export interface SetupImportResultResponse {
  imported: boolean;
  mode: 'EMPTY_ONLY';
  categoryCount: number;
  taskCount: number;
  activeTaskCount: number;
  inactiveTaskCount: number;
  warnings: string[];
  errors: string[];
}
