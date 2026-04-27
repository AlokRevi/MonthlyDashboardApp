package com.alok.monthlydashboard.repository;

import com.alok.monthlydashboard.entity.TaskCompletion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskCompletionRepository extends JpaRepository<TaskCompletion, Long> {

    boolean existsByTaskIdAndOccurrenceDate(Long taskId, LocalDate occurrenceDate);

    Optional<TaskCompletion> findByTaskIdAndOccurrenceDate(Long taskId, LocalDate occurrenceDate);

    List<TaskCompletion> findByTaskIdOrderByOccurrenceDateAsc(Long taskId);
}