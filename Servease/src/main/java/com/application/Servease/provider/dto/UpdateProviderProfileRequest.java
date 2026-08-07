package com.application.Servease.provider.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateProviderProfileRequest {

    @NotBlank
    private String businessName;

    @NotBlank
    private String description;

    @NotNull
    private Integer experienceYears;

    @NotBlank
    private String address;

    @NotBlank
    private String city;

    @NotBlank
    private String state;

    @NotBlank
    private String pincode;

    private Double latitude;

    private Double longitude;
}
