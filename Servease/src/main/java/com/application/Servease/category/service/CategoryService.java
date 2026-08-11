package com.application.Servease.category.service;

import com.application.Servease.category.dto.*;
import com.application.Servease.category.entity.Category;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(
            Long id,
            UpdateCategoryRequest request);

    void deleteCategory(Long id);

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(Long id);
    Category getCategoryEntity(Long id);
}