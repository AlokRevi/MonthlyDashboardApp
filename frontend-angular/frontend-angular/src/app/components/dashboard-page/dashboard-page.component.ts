import {
  ChangeDetectionStrategy,
  Component,
  OnInit
} from '@angular/core';

import { CommonModule } from '@angular/common';

import { TimelineStripComponent } from '../timeline-strip/timeline-strip.component';
import { CategorySectionComponent } from '../category-section/category-section.component';
import { TodayChecklistComponent } from '../today-checklist/today-checklist.component';
import { TaskCreateFormComponent } from '../task-create-form/task-create-form.component';
import { TaskEditModalComponent } from '../task-edit-modal/task-edit-modal.component';
import { CategoryManagerComponent } from '../category-manager/category-manager.component';
import { MonthNavigationComponent } from '../month-navigation/month-navigation.component';
import { CategoryCreateDialogComponent } from '../category-create-dialog/category-create-dialog.component';
import { QuarterTimelineGridComponent } from '../quarter-timeline-grid/quarter-timeline-grid.component';
import { QuadrimesterTimelineGridComponent } from '../quadrimester-timeline-grid/quadrimester-timeline-grid.component';
import { HalfyearTimelineGridComponent } from '../halfyear-timeline-grid/halfyear-timeline-grid.component';
import {
  INTERVAL_UNIT_OPTIONS,
  MONTH_OPTIONS,
  RECURRENCE_OPTIONS,
  WEEK_OF_MONTH_OPTIONS,
  WEEKDAY_OPTIONS
} from './dashboard-page.config';
import { DashboardPageStateService } from './dashboard-page-state.service';
import { ThemeId, ThemeService } from '../../services/theme.service';
import {
  ScaleNumbering,
  StartOfWeek,
  TimelineView
} from '../../models/dashboard.models';

interface ThemeOption {
  id: ThemeId;
  label: string;
}

interface ViewSettingOption<TValue extends string> {
  value: TValue;
  label: string;
}

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    TimelineStripComponent,
    CategorySectionComponent,
    TodayChecklistComponent,
    TaskCreateFormComponent,
    TaskEditModalComponent,
    CategoryManagerComponent,
    CategoryCreateDialogComponent,
    MonthNavigationComponent,
    QuarterTimelineGridComponent,
    QuadrimesterTimelineGridComponent,
    HalfyearTimelineGridComponent
  ],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [DashboardPageStateService]
})
export class DashboardPageComponent implements OnInit {
  protected readonly monthOptions = MONTH_OPTIONS;
  protected readonly recurrenceOptions = RECURRENCE_OPTIONS;
  protected readonly intervalUnitOptions = INTERVAL_UNIT_OPTIONS;
  protected readonly weekdayOptions = WEEKDAY_OPTIONS;
  protected readonly weekOfMonthOptions = WEEK_OF_MONTH_OPTIONS;
  protected readonly themeOptions: ThemeOption[] = [
    { id: 'theme-default', label: 'Default' },
    { id: 'theme-solar-punk', label: 'Solar Punk' },
    { id: 'theme-cyberpunk', label: 'Cyberpunk' },
    { id: 'theme-space-sci-fi', label: 'Space Sci-Fi' },
    { id: 'theme-matrix-coder', label: 'Matrix Coder' }
  ];
  protected readonly timelineViewOptions: ViewSettingOption<TimelineView>[] = [
    { value: 'MONTH', label: 'Month' },
    { value: 'QUARTER', label: 'Quarter' },
    { value: 'QUADRIMESTER', label: 'Quadrimester' },
    { value: 'HALF_YEAR', label: 'HalfYear' },
    { value: 'YEAR', label: 'Year' }
  ];
  protected readonly startOfWeekOptions: ViewSettingOption<StartOfWeek>[] = [
    { value: 'SUNDAY', label: 'Sunday' },
    { value: 'MONDAY', label: 'Monday' }
  ];
  protected readonly scaleNumberingOptions: ViewSettingOption<ScaleNumbering>[] = [
    { value: 'SEGMENT', label: 'Segment' },
    { value: 'CONTINUED', label: 'Continued' }
  ];

  constructor(
    protected state: DashboardPageStateService,
    protected themeService: ThemeService
  ) {}

  ngOnInit(): void {
    this.state.loadDashboard();
  }

  protected onThemeChange(event: Event): void {
    const selectedTheme = (event.target as HTMLSelectElement).value as ThemeId;

    this.themeService.setTheme(selectedTheme);
  }

  protected onTimelineViewChange(event: Event): void {
    this.state.setTimelineView((event.target as HTMLSelectElement).value as TimelineView);
  }

  protected onStartOfWeekChange(event: Event): void {
    this.state.setStartOfWeek((event.target as HTMLSelectElement).value as StartOfWeek);
  }

  protected onScaleNumberingChange(event: Event): void {
    this.state.setScaleNumbering((event.target as HTMLSelectElement).value as ScaleNumbering);
  }

  protected onCalendarYearBoundChange(event: Event): void {
    this.state.setCalendarYearBound((event.target as HTMLInputElement).checked);
  }
}
