package com.application.Servease.category.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateCategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    private String description;

    private String icon;

    private String image;

    private Integer displayOrder;

    private Boolean active;
}