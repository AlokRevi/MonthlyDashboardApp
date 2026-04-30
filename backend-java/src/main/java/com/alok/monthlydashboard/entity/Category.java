package com.alok.monthlydashboard.entity;

import com.alok.monthlydashboard.common.enums.CategoryRequires;
import com.alok.monthlydashboard.common.enums.FeelsLikeLabel;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "requires", nullable = false, length = 20, columnDefinition = "TEXT DEFAULT 'FOCUS'")
    private CategoryRequires requires = CategoryRequires.FOCUS;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "category_feels_like_labels",
            joinColumns = @JoinColumn(name = "category_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "label", nullable = false, length = 30)
    private List<FeelsLikeLabel> feelsLike = new ArrayList<>();

    @OneToMany(mappedBy = "category")
    private List<Task> tasks = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public CategoryRequires getRequires() {
        return requires;
    }

    public void setRequires(CategoryRequires requires) {
        this.requires = requires == null ? CategoryRequires.FOCUS : requires;
    }

    public List<FeelsLikeLabel> getFeelsLike() {
        return feelsLike;
    }

    public void setFeelsLike(List<FeelsLikeLabel> feelsLike) {
        this.feelsLike.clear();
        if (feelsLike != null) {
            this.feelsLike.addAll(feelsLike);
        }
    }

    public List<Task> getTasks() {
        return tasks;
    }
}
