package com.application.Servease.provider.service;

import com.application.Servease.provider.dto.*;
import com.application.Servease.provider.enums.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProviderService {

    ProviderProfileResponse becomeProvider(BecomeProviderRequest request);



    ProviderProfileResponse getMyProviderProfile();

    ProviderProfileResponse updateProviderProfile(UpdateProviderProfileRequest request);

    UploadDocumentResponse uploadDocument(
            MultipartFile file,
            DocumentType documentType,
            String documentNumber
    );




    void saveOrUpdateAvailability(List<SaveAvailabilityRequest> requests);

    List<AvailabilityResponse> getAvailability();
}