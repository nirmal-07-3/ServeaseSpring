package com.application.Servease.subcategory.service;

import com.application.Servease.category.entity.Category;
import com.application.Servease.category.service.CategoryService;
import com.application.Servease.common.exception.SubCategoryAlreadyExistsException;
import com.application.Servease.common.exception.SubCategoryNotFoundException;
import com.application.Servease.subcategory.dto.CreateSubCategoryRequest;
import com.application.Servease.subcategory.dto.SubCategoryResponse;
import com.application.Servease.subcategory.dto.UpdateSubCategoryRequest;
import com.application.Servease.subcategory.dto.UpdateSubCategoryStatusRequest;
import com.application.Servease.subcategory.entity.SubCategory;
import com.application.Servease.subcategory.mapper.SubCategoryMapper;
import com.application.Servease.subcategory.repository.SubCategoryRepository;
import com.application.Servease.subcategory.service.SubCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SubCategoryServiceImpl implements SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final SubCategoryMapper subCategoryMapper;
    private final CategoryService categoryService;

    @Override
    public SubCategoryResponse createSubCategory(
            CreateSubCategoryRequest request) {

        Category category = categoryService
                .getCategoryEntity(request.getCategoryId());

        if (subCategoryRepository.existsByCategoryAndNameIgnoreCase(
                category,
                request.getName())) {

            throw new SubCategoryAlreadyExistsException(
                    "SubCategory already exists in this category."
            );
        }

        SubCategory subCategory =
                subCategoryMapper.toEntity(request, category);

        SubCategory savedSubCategory =
                subCategoryRepository.save(subCategory);

        return subCategoryMapper.toResponse(savedSubCategory);
    }

    @Override
    public SubCategoryResponse updateSubCategory(
            Long id,
            UpdateSubCategoryRequest request) {

        SubCategory subCategory =
                getSubCategoryEntity(id);

        if (!subCategory.getName().equalsIgnoreCase(request.getName())
                && subCategoryRepository.existsByCategoryAndNameIgnoreCase(
                subCategory.getCategory(),
                request.getName())) {

            throw new SubCategoryAlreadyExistsException(
                    "SubCategory already exists in this category."
            );
        }

        subCategoryMapper.updateEntity(request, subCategory);

        SubCategory updatedSubCategory =
                subCategoryRepository.save(subCategory);

        return subCategoryMapper.toResponse(updatedSubCategory);
    }


    @Override
    public void deleteSubCategory(Long id) {

        SubCategory subCategory = getSubCategoryEntity(id);

        subCategory.setActive(false);

        subCategoryRepository.save(subCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubCategoryResponse> getAllSubCategories() {

        return subCategoryRepository
                .findAllByOrderByDisplayOrderAscNameAsc()
                .stream()
                .map(subCategoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubCategoryResponse> getSubCategoriesByCategory(
            Long categoryId) {

        Category category = categoryService
                .getCategoryEntity(categoryId);

        return subCategoryRepository
                .findByCategoryAndActiveTrueOrderByDisplayOrderAscNameAsc(category)
                .stream()
                .map(subCategoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubCategoryResponse getSubCategoryById(Long id) {

        return subCategoryMapper.toResponse(
                getActiveSubCategoryEntity(id)
        );
    }

    @Override
    public void updateSubCategoryStatus(
            Long id,
            UpdateSubCategoryStatusRequest request) {

        SubCategory subCategory = getSubCategoryEntity(id);

        subCategory.setActive(request.getActive());

        subCategoryRepository.save(subCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public SubCategory getSubCategoryEntity(Long id) {

        return subCategoryRepository.findById(id)
                .orElseThrow(() ->
                        new SubCategoryNotFoundException(
                                "SubCategory not found."
                        ));
    }

    @Override
    @Transactional(readOnly = true)
    public SubCategory getActiveSubCategoryEntity(Long id) {

        return subCategoryRepository
                .findByIdAndActiveTrue(id)
                .orElseThrow(() ->
                        new SubCategoryNotFoundException(
                                "SubCategory not found."
                        ));
    }



}