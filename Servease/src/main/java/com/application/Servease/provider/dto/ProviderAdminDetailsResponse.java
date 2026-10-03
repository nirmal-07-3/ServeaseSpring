package com.application.Servease.provider.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderAdminDetailsResponse {

    private Long id;

    private String businessName;

    private String description;

    private String city;

    private String state;

    private String country;

    private String pincode;

    private Integer experienceYears;

    private Double latitude;

    private Double longitude;

    private Boolean profileCompleted;

    private Double averageRating;

    private Integer totalReviews;

    private Integer totalBookings;

    private String verificationStatus;

    private List<UploadDocumentResponse> documents;
}