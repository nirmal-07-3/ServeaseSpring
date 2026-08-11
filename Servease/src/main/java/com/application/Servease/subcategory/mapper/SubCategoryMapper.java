package com.application.Servease.subcategory.mapper;

import com.application.Servease.category.entity.Category;
import com.application.Servease.subcategory.dto.CreateSubCategoryRequest;
import com.application.Servease.subcategory.dto.SubCategoryResponse;
import com.application.Servease.subcategory.dto.UpdateSubCategoryRequest;
import com.application.Servease.subcategory.entity.SubCategory;
import org.springframework.stereotype.Component;

@Component
public class SubCategoryMapper {

    /**
     * Convert CreateSubCategoryRequest to Entity.
     */
    public SubCategory toEntity(CreateSubCategoryRequest request,
                                Category category) {

        return SubCategory.builder()
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .displayOrder(
                        request.getDisplayOrder() == null
                                ? 0
                                : request.getDisplayOrder()
                )
                .active(true)
                .build();
    }

    /**
     * Update existing SubCategory entity.
     */
    public void updateEntity(UpdateSubCategoryRequest request,
                             SubCategory subCategory) {

        subCategory.setName(request.getName());
        subCategory.setDescription(request.getDescription());
        subCategory.setIcon(request.getIcon());
        if (request.getDisplayOrder() != null) {
            subCategory.setDisplayOrder(request.getDisplayOrder());
        }
        subCategory.setActive(request.getActive());
    }

    /**
     * Convert Entity to Response DTO.
     */
    public SubCategoryResponse toResponse(SubCategory subCategory) {

        return SubCategoryResponse.builder()
                .id(subCategory.getId())
                .categoryId(subCategory.getCategory().getId())
                .categoryName(subCategory.getCategory().getName())
                .name(subCategory.getName())
                .description(subCategory.getDescription())
                .icon(subCategory.getIcon())
                .displayOrder(subCategory.getDisplayOrder())
                .active(subCategory.getActive())
                .build();
    }
}