package com.application.Servease.category.service;

import com.application.Servease.category.dto.CategoryResponse;
import com.application.Servease.category.dto.CreateCategoryRequest;
import com.application.Servease.category.dto.UpdateCategoryRequest;
import com.application.Servease.category.entity.Category;
import com.application.Servease.category.mapper.CategoryMapper;
import com.application.Servease.category.repository.CategoryRepository;
import com.application.Servease.common.exception.CategoryAlreadyExistsException;
import com.application.Servease.common.exception.CategoryNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {

        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {

            throw new CategoryAlreadyExistsException(
                    "Category already exists."
            );
        }

        Category category = categoryMapper.toEntity(request);

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    public List<CategoryResponse> getAllCategories() {

        return categoryRepository
                .findByActiveTrueOrderByDisplayOrderAscNameAsc()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new CategoryNotFoundException(
                                "Category not found."
                        ));

        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(
            Long id,
            UpdateCategoryRequest request) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new CategoryNotFoundException(
                                "Category not found."
                        ));

        if (!category.getName().equalsIgnoreCase(request.getName())
                && categoryRepository.existsByNameIgnoreCase(request.getName())) {

            throw new CategoryAlreadyExistsException(
                    "Category already exists."
            );
        }

        categoryMapper.updateEntity(request, category);

        Category updatedCategory = categoryRepository.save(category);

        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new CategoryNotFoundException(
                                "Category not found."
                        ));

        category.setActive(false);

        categoryRepository.save(category);
    }

    List<Category> findAllByOrderByDisplayOrderAscNameAsc(){
       return null;
    }

    Optional<Category> findByIdAndActiveTrue(Long id){
        return null;
    }

}