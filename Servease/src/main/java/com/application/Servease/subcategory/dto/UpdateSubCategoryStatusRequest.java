package com.application.Servease.subcategory.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateSubCategoryStatusRequest {

    @NotNull(message = "Status is required")
    private Boolean active;
}