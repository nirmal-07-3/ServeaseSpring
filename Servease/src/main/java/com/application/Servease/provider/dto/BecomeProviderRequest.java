package com.application.Servease.provider.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BecomeProviderRequest {

    @NotBlank
    private String businessName;

    private String description;

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