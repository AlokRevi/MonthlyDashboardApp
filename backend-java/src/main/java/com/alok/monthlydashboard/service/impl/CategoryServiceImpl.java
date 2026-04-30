package com.alok.monthlydashboard.service.impl;

import com.alok.monthlydashboard.dto.category.CategoryResponse;
import com.alok.monthlydashboard.dto.category.CreateCategoryRequest;
import com.alok.monthlydashboard.dto.category.UpdateCategoryRequest;
import com.alok.monthlydashboard.entity.Category;
import com.alok.monthlydashboard.exception.ConflictException;
import com.alok.monthlydashboard.exception.ResourceNotFoundException;
import com.alok.monthlydashboard.repository.CategoryRepository;
import com.alok.monthlydashboard.service.CategoryService;
import com.alok.monthlydashboard.util.CategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        Category category = new Category();
        category.setName(request.name());
        category.setColor(request.color());
        category.setRequires(request.requires());
        category.setFeelsLike(request.feelsLike());

        Category saved = categoryRepository.save(category);
        return CategoryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long categoryId) {
        Category category = getCategoryOrThrow(categoryId);
        return CategoryMapper.toResponse(category);
    }

    @Override
    public CategoryResponse updateCategory(Long categoryId, UpdateCategoryRequest request) {
        Category category = getCategoryOrThrow(categoryId);
        category.setName(request.name());
        category.setColor(request.color());
        category.setRequires(request.requires());
        category.setFeelsLike(request.feelsLike());

        Category saved = categoryRepository.save(category);
        return CategoryMapper.toResponse(saved);
    }

    @Override
    public void deleteCategory(Long categoryId) {
        Category category = getCategoryOrThrow(categoryId);

        if (!category.getTasks().isEmpty()) {
            throw new ConflictException("Category cannot be deleted while tasks still belong to it");
        }

        categoryRepository.delete(category);
    }

    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }
}
