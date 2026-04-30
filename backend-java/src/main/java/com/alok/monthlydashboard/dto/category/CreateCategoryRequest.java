package com.alok.monthlydashboard.dto.category;

import com.alok.monthlydashboard.common.enums.CategoryRequires;
import com.alok.monthlydashboard.common.enums.FeelsLikeLabel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateCategoryRequest(

        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @Size(max = 20, message = "color must be at most 20 characters")
        String color,

        CategoryRequires requires,

        List<FeelsLikeLabel> feelsLike
) {
}
