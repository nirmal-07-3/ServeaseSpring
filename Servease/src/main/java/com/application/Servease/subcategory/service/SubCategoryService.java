package com.application.Servease.subcategory.service;

import com.application.Servease.subcategory.dto.CreateSubCategoryRequest;
import com.application.Servease.subcategory.dto.SubCategoryResponse;
import com.application.Servease.subcategory.dto.UpdateSubCategoryRequest;
import com.application.Servease.subcategory.dto.UpdateSubCategoryStatusRequest;
import com.application.Servease.subcategory.entity.SubCategory;

import java.util.List;

public interface SubCategoryService {

    SubCategoryResponse createSubCategory(CreateSubCategoryRequest request);

    SubCategoryResponse updateSubCategory(
            Long id,
            UpdateSubCategoryRequest request);

    void deleteSubCategory(Long id);

    List<SubCategoryResponse> getAllSubCategories();

    List<SubCategoryResponse> getSubCategoriesByCategory(Long categoryId);

    SubCategoryResponse getSubCategoryById(Long id);

    void updateSubCategoryStatus(
            Long id,
            UpdateSubCategoryStatusRequest request);

    SubCategory getSubCategoryEntity(Long id);

    SubCategory getActiveSubCategoryEntity(Long id);
}