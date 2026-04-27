package com.alok.monthlydashboard.entity;

import com.alok.monthlydashboard.common.enums.RecurrenceType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
public class Task extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 20)
    private RecurrenceType recurrenceType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @OneToOne(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TaskRecurrenceRule recurrenceRule;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfMonth ASC")
    private List<TaskFixedDate> fixedDates = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("occurrenceDate ASC")
    private List<TaskCompletion> completions = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RecurrenceType getRecurrenceType() {
        return recurrenceType;
    }

    public void setRecurrenceType(RecurrenceType recurrenceType) {
        this.recurrenceType = recurrenceType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public TaskRecurrenceRule getRecurrenceRule() {
        return recurrenceRule;
    }

    public void setRecurrenceRule(TaskRecurrenceRule recurrenceRule) {
        this.recurrenceRule = recurrenceRule;
        if (recurrenceRule != null) {
            recurrenceRule.setTask(this);
        }
    }

    public List<TaskFixedDate> getFixedDates() {
        return fixedDates;
    }

    public void addFixedDate(TaskFixedDate fixedDate) {
        this.fixedDates.add(fixedDate);
        fixedDate.setTask(this);
    }

    public void clearFixedDates() {
        for (TaskFixedDate fixedDate : fixedDates) {
            fixedDate.setTask(null);
        }
        fixedDates.clear();
    }

    public List<TaskCompletion> getCompletions() {
        return completions;
    }

    public void addCompletion(TaskCompletion completion) {
        this.completions.add(completion);
        completion.setTask(this);
    }
}