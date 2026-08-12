package com.application.Servease.providerservice.mapper;

import com.application.Servease.providerservice.dto.CreateProviderServiceRequest;
import com.application.Servease.providerservice.dto.ProviderServiceResponse;
import com.application.Servease.providerservice.dto.UpdateProviderServiceRequest;
import com.application.Servease.providerservice.entity.ProviderService;
import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.subcategory.entity.SubCategory;
import org.springframework.stereotype.Component;

@Component
public class ProviderServiceMapper {

    public ProviderService toEntity(
            CreateProviderServiceRequest request,
            ProviderProfile provider,
            SubCategory subCategory) {

        return ProviderService.builder()
                .provider(provider)
                .subCategory(subCategory)
                .title(request.getTitle())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .estimatedDurationMinutes(
                        request.getEstimatedDurationMinutes()
                )
                .serviceRadiusKm(request.getServiceRadiusKm())
                .active(true)
                .build();
    }

    public void updateEntity(
            UpdateProviderServiceRequest request,
            ProviderService providerService) {

        providerService.setTitle(request.getTitle());
        providerService.setDescription(request.getDescription());
        providerService.setBasePrice(request.getBasePrice());
        providerService.setEstimatedDurationMinutes(
                request.getEstimatedDurationMinutes()
        );
        providerService.setServiceRadiusKm(
                request.getServiceRadiusKm()
        );
    }

    public ProviderServiceResponse toResponse(
            ProviderService providerService) {

        return ProviderServiceResponse.builder()
                .id(providerService.getId())

                .providerId(
                        providerService.getProvider().getId()
                )

                .providerBusinessName(
                        providerService.getProvider().getBusinessName()
                )

                .subCategoryId(
                        providerService.getSubCategory().getId()
                )

                .subCategoryName(
                        providerService.getSubCategory().getName()
                )

                .title(providerService.getTitle())
                .description(providerService.getDescription())
                .basePrice(providerService.getBasePrice())
                .estimatedDurationMinutes(
                        providerService.getEstimatedDurationMinutes()
                )
                .serviceRadiusKm(
                        providerService.getServiceRadiusKm()
                )
                .active(providerService.getActive())

                .build();
    }
}