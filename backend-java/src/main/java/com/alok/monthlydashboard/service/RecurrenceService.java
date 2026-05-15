package com.alok.monthlydashboard.service;

import com.alok.monthlydashboard.dto.dashboard.OccurrenceResponse;
import com.alok.monthlydashboard.entity.Task;

import java.time.LocalDate;
import java.util.List;

public interface RecurrenceService {
    List<OccurrenceResponse> generateOccurrencesForMonth(Long taskId, int year, int month);
    List<LocalDate> generateOccurrenceDatesForMonth(Long taskId, int year, int month);
    List<OccurrenceResponse> generateOccurrencesBetween(Task task, LocalDate startDate, LocalDate endDate);
    List<OccurrenceResponse> generateOccurrencesBetween(Long taskId, LocalDate startDate, LocalDate endDate);
    List<LocalDate> generateOccurrenceDatesBetween(Task task, LocalDate startDate, LocalDate endDate);
    List<LocalDate> generateOccurrenceDatesBetween(Long taskId, LocalDate startDate, LocalDate endDate);
    boolean isValidOccurrence(Long taskId, LocalDate occurrenceDate);
}
