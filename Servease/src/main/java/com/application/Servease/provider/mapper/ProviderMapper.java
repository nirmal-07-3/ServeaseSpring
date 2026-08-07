package com.application.Servease.provider.mapper;


import com.application.Servease.provider.dto.BecomeProviderRequest;
import com.application.Servease.provider.dto.ProviderProfileResponse;
import com.application.Servease.provider.entity.ProviderProfile;
import org.springframework.stereotype.Component;

@Component
    public class ProviderMapper {

        public ProviderProfile toEntity(BecomeProviderRequest request) {

            return ProviderProfile.builder()
                    .businessName(request.getBusinessName())
                    .description(request.getDescription())
                    .experienceYears(request.getExperienceYears())
                    .address(request.getAddress())
                    .city(request.getCity())
                    .state(request.getState())
                    .country("India")
                    .pincode(request.getPincode())
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .build();
        }

        public ProviderProfileResponse toResponse(ProviderProfile provider) {

            return ProviderProfileResponse.builder()
                    .id(provider.getId())
                    .businessName(provider.getBusinessName())
                    .description(provider.getDescription())
                    .experienceYears(provider.getExperienceYears())
                    .verificationStatus(provider.getVerificationStatus())
                    .profileCompleted(provider.getProfileCompleted())
                    .averageRating(provider.getAverageRating())
                    .totalReviews(provider.getTotalReviews())
                    .totalBookings(provider.getTotalBookings())
                    .address(provider.getAddress())
                    .city(provider.getCity())
                    .state(provider.getState())
                    .country(provider.getCountry())
                    .pincode(provider.getPincode())
                    .build();
        }
    }

