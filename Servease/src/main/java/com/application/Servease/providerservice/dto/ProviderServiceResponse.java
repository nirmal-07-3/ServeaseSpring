package com.application.Servease.providerservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProviderServiceResponse {

    private Long id;

    private Long providerId;

    private String providerBusinessName;

    private Long subCategoryId;

    private String subCategoryName;

    private String title;

    private String description;

    private BigDecimal basePrice;

    private Integer estimatedDurationMinutes;

    private Double serviceRadiusKm;

    private Boolean active;
}