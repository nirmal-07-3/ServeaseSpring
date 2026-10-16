
package com.application.Servease.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DevelopmentCollectionSimulationRequest {

    @NotBlank
    private String outcome;
}
