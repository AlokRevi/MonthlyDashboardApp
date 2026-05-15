import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

import {
  MonthlyDashboardResponse,
  TodayChecklistResponse,
  CompleteTaskRequest,
  CreateCategoryRequest,
  CategoryResponse,
  CreateTaskRequest,
  UpdateCategoryRequest,
  UpdateTaskRequest,
  TaskResponse,
  SetupImportPreviewResponse,
  SetupImportResultResponse,
  SetupSnapshotResponse,
  TimelineDashboardResponse
} from '../models/dashboard.models';

@Injectable({
  providedIn: 'root'
})
export class DashboardApiService {
  private readonly apiBase = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  getMonthlyDashboard(year: number, month: number): Observable<MonthlyDashboardResponse> {
    return this.http.get<MonthlyDashboardResponse>(
      `${this.apiBase}/dashboard/monthly?year=${year}&month=${month}`
    );
  }

  getMonthTimelineDashboard(): Observable<TimelineDashboardResponse> {
    return this.http.get<TimelineDashboardResponse>(
      `${this.apiBase}/dashboard/timeline?view=MONTH`
    );
  }

  getTodayChecklist(): Observable<TodayChecklistResponse> {
    return this.http.get<TodayChecklistResponse>(
      `${this.apiBase}/checklist/today`
    );
  }

  getCategories(): Observable<CategoryResponse[]> {
    return this.http.get<CategoryResponse[]>(
      `${this.apiBase}/categories`
    );
  }

  getSetupSnapshot(): Observable<SetupSnapshotResponse> {
    return this.http.get<SetupSnapshotResponse>(
      `${this.apiBase}/export/setup`
    );
  }

  previewSetupImport(snapshot: unknown): Observable<SetupImportPreviewResponse> {
    return this.http.post<SetupImportPreviewResponse>(
      `${this.apiBase}/import/setup/preview`,
      snapshot
    );
  }

  importSetupSnapshot(snapshot: unknown): Observable<SetupImportResultResponse> {
    return this.http.post<SetupImportResultResponse>(
      `${this.apiBase}/import/setup?mode=EMPTY_ONLY`,
      snapshot
    );
  }

  createCategory(request: CreateCategoryRequest): Observable<CategoryResponse> {
    return this.http.post<CategoryResponse>(
      `${this.apiBase}/categories`,
      request
    );
  }

  updateCategory(categoryId: number, request: UpdateCategoryRequest): Observable<CategoryResponse> {
    return this.http.put<CategoryResponse>(
      `${this.apiBase}/categories/${categoryId}`,
      request
    );
  }

  deleteCategory(categoryId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiBase}/categories/${categoryId}`
    );
  }

  createTask(request: CreateTaskRequest): Observable<TaskResponse> {
    return this.http.post<TaskResponse>(
      `${this.apiBase}/tasks`,
      request
    );
  }

  getTask(taskId: number): Observable<TaskResponse> {
    return this.http.get<TaskResponse>(
      `${this.apiBase}/tasks/${taskId}`
    );
  }

  updateTask(taskId: number, request: UpdateTaskRequest): Observable<TaskResponse> {
    return this.http.put<TaskResponse>(
      `${this.apiBase}/tasks/${taskId}`,
      request
    );
  }

  deleteTask(taskId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiBase}/tasks/${taskId}?deleteHistory=false`
    );
  }

  completeTask(taskId: number, request: CompleteTaskRequest): Observable<unknown> {
    return this.http.post(
      `${this.apiBase}/tasks/${taskId}/completions`,
      request
    );
  }

  undoCompletion(taskId: number, occurrenceDate: string): Observable<void> {
    return this.http.delete<void>(
      `${this.apiBase}/tasks/${taskId}/completions/${occurrenceDate}`
    );
  }
}
