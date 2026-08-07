package com.application.Servease.category.mapper;

import com.application.Servease.category.dto.CategoryResponse;
import com.application.Servease.category.dto.CreateCategoryRequest;
import com.application.Servease.category.dto.UpdateCategoryRequest;
import com.application.Servease.category.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toEntity(CreateCategoryRequest request) {

        return Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .image(request.getImage())
                .displayOrder(
                        request.getDisplayOrder() == null ? 0 : request.getDisplayOrder()
                )
                .active(true)
                .build();
    }

    public void updateEntity(UpdateCategoryRequest request,
                             Category category) {

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setImage(request.getImage());
        category.setDisplayOrder(request.getDisplayOrder());
        category.setActive(request.getActive());
    }

    public CategoryResponse toResponse(Category category) {

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .icon(category.getIcon())
                .image(category.getImage())
                .displayOrder(category.getDisplayOrder())
                .active(category.getActive())
                .build();
    }
}