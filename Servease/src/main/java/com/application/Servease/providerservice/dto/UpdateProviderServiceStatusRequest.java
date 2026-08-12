package com.application.Servease.providerservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateProviderServiceStatusRequest {

    @NotNull(message = "Status is required")
    private Boolean active;
}