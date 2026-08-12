package com.application.Servease.providerservice.service;

import com.application.Servease.providerservice.dto.CreateProviderServiceRequest;
import com.application.Servease.providerservice.dto.ProviderServiceResponse;
import com.application.Servease.providerservice.dto.UpdateProviderServiceRequest;
import com.application.Servease.providerservice.dto.UpdateProviderServiceStatusRequest;
import com.application.Servease.providerservice.entity.ProviderService;

import java.util.List;

public interface ProviderServiceService {

    ProviderServiceResponse createService(
            CreateProviderServiceRequest request
    );

    ProviderServiceResponse updateService(
            Long id,
            UpdateProviderServiceRequest request
    );

    void deleteService(Long id);

    ProviderServiceResponse getMyService(Long id);

    List<ProviderServiceResponse> getMyServices();

    void updateServiceStatus(
            Long id,
            UpdateProviderServiceStatusRequest request
    );

    List<ProviderServiceResponse> getServicesBySubCategory(
            Long subCategoryId
    );

    ProviderService getServiceEntity(Long id);
}