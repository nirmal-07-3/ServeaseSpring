package com.application.Servease.category.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryResponse {

    private Long id;

    private String name;

    private String description;

    private String icon;

    private String image;

    private Integer displayOrder;

    private Boolean active;
}