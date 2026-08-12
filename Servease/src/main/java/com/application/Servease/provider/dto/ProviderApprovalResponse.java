package com.application.Servease.provider.dto;

import com.application.Servease.provider.enums.VerificationStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProviderApprovalResponse {

    private Long id;

    private String businessName;

    private String description;

    private String city;

    private String state;

    private String country;

    private String pincode;

    private Integer experienceYears;

    private VerificationStatus verificationStatus;

    private Boolean profileCompleted;
}