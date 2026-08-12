package com.application.Servease.providerservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateProviderServiceRequest {

    @NotBlank(message = "Service title is required")
    private String title;

    private String description;

    @NotNull(message = "Base price is required")
    @DecimalMin(
            value = "0.01",
            message = "Base price must be greater than 0"
    )
    private BigDecimal basePrice;

    @NotNull(message = "Estimated duration is required")
    @Positive(message = "Estimated duration must be greater than 0")
    private Integer estimatedDurationMinutes;

    @NotNull(message = "Service radius is required")
    @DecimalMin(
            value = "0.1",
            message = "Service radius must be greater than 0"
    )
    private Double serviceRadiusKm;
}