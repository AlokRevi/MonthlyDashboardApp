package com.alok.monthlydashboard.repository;

import com.alok.monthlydashboard.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByOrderByNameAsc();

    List<Task> findByCategoryIdOrderByNameAsc(Long categoryId);

    List<Task> findByIsActiveOrderByNameAsc(boolean isActive);

    List<Task> findByCategoryIdAndIsActiveOrderByNameAsc(Long categoryId, boolean isActive);

    long countByIsActive(boolean isActive);
}