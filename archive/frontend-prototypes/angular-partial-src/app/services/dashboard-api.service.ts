import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  MonthlyDashboardResponse,
  TodayChecklistResponse,
  CompleteTaskRequest
} from '../models/dashboard.models';
import { Observable, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DashboardApiService {
  private readonly apiBase = 'http://localhost:8080/api/v1';
  private readonly useMock = true;

  constructor(private http: HttpClient) {}

  getMonthlyDashboard(year: number, month: number): Observable<MonthlyDashboardResponse> {
    if (this.useMock) {
      return of(this.buildMockDashboard(year, month));
    }

    return this.http.get<MonthlyDashboardResponse>(
      `${this.apiBase}/dashboard/monthly?year=${year}&month=${month}`
    );
  }

  getTodayChecklist(): Observable<TodayChecklistResponse> {
    if (this.useMock) {
      return of(this.buildMockChecklist());
    }

    return this.http.get<TodayChecklistResponse>(`${this.apiBase}/checklist/today`);
  }

  completeTask(taskId: number, request: CompleteTaskRequest): Observable<unknown> {
    if (this.useMock) {
      console.log('Mock complete', taskId, request);
      return of({});
    }

    return this.http.post(`${this.apiBase}/tasks/${taskId}/completions`, request);
  }

  private buildMockDashboard(year: number, month: number): MonthlyDashboardResponse {
    const lastDay = new Date(year, month, 0).getDate();
    const today = new Date();
    const isCurrentMonth =
      today.getFullYear() === year && today.getMonth() + 1 === month;

    const weekdayNames = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

    const dayStrip = Array.from({ length: lastDay }, (_, index) => {
      const day = index + 1;
      const d = new Date(year, month - 1, day);
      const weekdayIndex = d.getDay();

      return {
        date: this.toDateStr(d),
        dayOfMonth: day,
        weekday: weekdayNames[weekdayIndex],
        isToday: isCurrentMonth && day === today.getDate(),
        isWeekend: weekdayIndex === 0 || weekdayIndex === 6
      };
    });

    return {
      year,
      month,
      monthLabel: `${this.monthName(month)} ${year}`,
      today: this.toDateStr(today),
      readOnly: false,
      scaleBar: {
        anchors: [1, 8, 15, 22, lastDay],
        lastDay,
        currentDateLabel: isCurrentMonth
          ? `${this.monthName(month).slice(0, 3)} ${today.getDate()}`
          : `${this.monthName(month).slice(0, 3)} ${year}`
      },
      dayStrip,
      categories: [
        {
          categoryId: 1,
          categoryName: 'Bills',
          tasks: [
            {
              taskId: 101,
              taskName: 'Credit Card Bill',
              recurrenceType: 'FIXED_DATE',
              occurrences: [
                {
                  occurrenceDate: `${year}-${this.pad(month)}-05`,
                  dayOfMonth: 5,
                  completed: true,
                  completionDate: `${year}-${this.pad(month)}-06`,
                  status: 'COMPLETED'
                },
                {
                  occurrenceDate: `${year}-${this.pad(month)}-28`,
                  dayOfMonth: 28,
                  completed: false,
                  completionDate: null,
                  status: 'UPCOMING'
                }
              ]
            }
          ]
        },
        {
          categoryId: 2,
          categoryName: 'Plants',
          tasks: [
            {
              taskId: 201,
              taskName: 'Plant Watering',
              recurrenceType: 'INTERVAL',
              occurrences: [
                {
                  occurrenceDate: `${year}-${this.pad(month)}-03`,
                  dayOfMonth: 3,
                  completed: true,
                  completionDate: `${year}-${this.pad(month)}-03`,
                  status: 'COMPLETED'
                },
                {
                  occurrenceDate: `${year}-${this.pad(month)}-10`,
                  dayOfMonth: 10,
                  completed: false,
                  completionDate: null,
                  status: 'OVERDUE'
                },
                {
                  occurrenceDate: `${year}-${this.pad(month)}-17`,
                  dayOfMonth: 17,
                  completed: false,
                  completionDate: null,
                  status: 'UPCOMING'
                }
              ].filter(item => item.dayOfMonth <= lastDay)
            }
          ]
        }
      ]
    };
  }

  private buildMockChecklist(): TodayChecklistResponse {
    const today = new Date();

    return {
      today: this.toDateStr(today),
      items: [
        {
          taskId: 201,
          taskName: 'Plant Watering',
          categoryId: 2,
          categoryName: 'Plants',
          occurrenceDate: this.toDateStr(today),
          status: 'DUE_TODAY'
        },
        {
          taskId: 101,
          taskName: 'Credit Card Bill',
          categoryId: 1,
          categoryName: 'Bills',
          occurrenceDate: '2026-04-05',
          status: 'OVERDUE'
        }
      ]
    };
  }

  private monthName(month: number): string {
    const names = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ];
    return names[month - 1];
  }

  private toDateStr(date: Date): string {
    return `${date.getFullYear()}-${this.pad(date.getMonth() + 1)}-${this.pad(date.getDate())}`;
  }

  private pad(value: number): string {
    return String(value).padStart(2, '0');
  }
}