package com.application.Servease.subcategory.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubCategoryResponse {

    private Long id;

    private Long categoryId;

    private String categoryName;

    private String name;

    private String description;

    private String icon;

    private Integer displayOrder;

    private Boolean active;
}