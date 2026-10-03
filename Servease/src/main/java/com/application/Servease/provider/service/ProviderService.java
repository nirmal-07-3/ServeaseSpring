package com.application.Servease.provider.service;

import com.application.Servease.provider.dto.*;
import com.application.Servease.provider.entity.ProviderAvailability;
import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.provider.enums.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface ProviderService {

    ProviderProfileResponse becomeProvider(
            BecomeProviderRequest request
    );

    ProviderProfileResponse getMyProviderProfile();

    ProviderProfileResponse updateProviderProfile(
            UpdateProviderProfileRequest request
    );

    UploadDocumentResponse uploadDocument(
            MultipartFile file,
            DocumentType documentType,
            String documentNumber
    );

    List<UploadDocumentResponse> getMyDocuments();

    void saveOrUpdateAvailability(
            List<SaveAvailabilityRequest> requests
    );

    List<AvailabilityResponse> getAvailability();

    Optional<ProviderAvailability> findByProviderAndDayOfWeek(
            ProviderProfile provider,
            DayOfWeek dayOfWeek
    );

    Optional<ProviderAvailability> getAvailabilityForDay(
            ProviderProfile provider,
            DayOfWeek dayOfWeek
    );
}