package com.application.Servease.subcategory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateSubCategoryRequest {

    @NotNull(message = "Category id is required")
    private Long categoryId;

    @NotBlank(message = "SubCategory name is required")
    private String name;

    private String description;

    private String icon;

    private Integer displayOrder;

    private Boolean active;
}