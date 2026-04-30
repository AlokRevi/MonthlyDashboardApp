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
import {
  INTERVAL_UNIT_OPTIONS,
  MONTH_OPTIONS,
  RECURRENCE_OPTIONS,
  WEEK_OF_MONTH_OPTIONS,
  WEEKDAY_OPTIONS
} from './dashboard-page.config';
import { DashboardPageStateService } from './dashboard-page-state.service';

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
    MonthNavigationComponent
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

  constructor(protected state: DashboardPageStateService) {}

  ngOnInit(): void {
    this.state.loadDashboard();
  }
}
