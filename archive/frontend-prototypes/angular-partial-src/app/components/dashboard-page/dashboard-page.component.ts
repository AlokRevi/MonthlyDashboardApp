import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import {
  MonthlyDashboardResponse,
  TodayChecklistResponse
} from '../../models/dashboard.models';
import { DashboardApiService } from '../../services/dashboard-api.service';
import { TimelineStripComponent } from '../timeline-strip/timeline-strip.component';
import { CategorySectionComponent } from '../category-section/category-section.component';
import { TodayChecklistComponent } from '../today-checklist/today-checklist.component';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TimelineStripComponent,
    CategorySectionComponent,
    TodayChecklistComponent
  ],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.css'
})
export class DashboardPageComponent implements OnInit {
  monthOptions = [
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

  selectedYear = new Date().getFullYear();
  selectedMonth = new Date().getMonth() + 1;

  dashboard: MonthlyDashboardResponse | null = null;
  checklist: TodayChecklistResponse | null = null;
  loading = false;
  errorMessage = '';

  constructor(private dashboardApi: DashboardApiService) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      dashboard: this.dashboardApi.getMonthlyDashboard(this.selectedYear, this.selectedMonth),
      checklist: this.dashboardApi.getTodayChecklist()
    }).subscribe({
      next: ({ dashboard, checklist }) => {
        this.dashboard = dashboard;
        this.checklist = checklist;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Could not load dashboard.';
        this.loading = false;
      }
    });
  }

  onMarkComplete(event: { taskId: number; occurrenceDate: string }): void {
    const today = new Date().toISOString().slice(0, 10);

    this.dashboardApi.completeTask(event.taskId, {
      occurrenceDate: event.occurrenceDate,
      completionDate: today
    }).subscribe({
      next: () => this.loadDashboard(),
      error: () => {
        this.errorMessage = 'Could not mark task complete.';
      }
    });
  }
}
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'app-timeline-strip',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './timeline-strip.component.html',
  styleUrl: './timeline-strip.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})