package com.application.Servease.provider.service;

import com.application.Servease.common.exception.DuplicateDocumentException;
import com.application.Servease.common.exception.InvalidAvailabilityException;
import com.application.Servease.common.exception.ProviderAlreadyExistsException;
import com.application.Servease.common.exception.ProviderNotFoundException;
import com.application.Servease.common.storage.StorageService;
import com.application.Servease.provider.dto.*;
import com.application.Servease.provider.entity.ProviderAvailability;
import com.application.Servease.provider.entity.ProviderDocument;
import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.provider.enums.DocumentType;
import com.application.Servease.provider.enums.VerificationStatus;
import com.application.Servease.provider.mapper.ProviderAvailabilityMapper;
import com.application.Servease.provider.mapper.ProviderDocumentMapper;
import com.application.Servease.provider.mapper.ProviderMapper;
import com.application.Servease.provider.repository.ProviderAvailabilityRepository;
import com.application.Servease.provider.repository.ProviderDocumentRepository;
import com.application.Servease.provider.repository.ProviderProfileRepository;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.enums.UserRole;
import com.application.Servease.user.repository.UserRepository;
import com.application.Servease.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProviderServiceImpl implements ProviderService {

    private final ProviderProfileRepository providerProfileRepository;
    private final UserService userService;
    private final ProviderMapper providerMapper;

    private final ProviderDocumentRepository providerDocumentRepository;
    private final ProviderDocumentMapper providerDocumentMapper;
    private final StorageService storageService;

    private final ProviderAvailabilityRepository providerAvailabilityRepository;

    private final ProviderAvailabilityMapper providerAvailabilityMapper;

    @Override
    @Transactional
    public ProviderProfileResponse becomeProvider(
            BecomeProviderRequest request) {

        User user = userService.getAuthenticatedUser();

        if (providerProfileRepository.existsByUser(user)) {
            throw new ProviderAlreadyExistsException(
                    "You are already registered as a provider."
            );
        }

        ProviderProfile provider =
                providerMapper.toEntity(request);

        provider.setUser(user);
        provider.setVerificationStatus(
                VerificationStatus.PENDING
        );
        provider.setProfileCompleted(false);
        provider.setAverageRating(0.0);
        provider.setTotalReviews(0);
        provider.setTotalBookings(0);
        provider.setCountry("India");

        // Change USER role from CUSTOMER to PROVIDER
        user.setRole(UserRole.PROVIDER);

        // Save updated user
        userService.updateRole(
                user,
                UserRole.PROVIDER
        );

        // Save provider profile
        ProviderProfile savedProvider =
                providerProfileRepository.save(provider);

        return providerMapper.toResponse(savedProvider);
    }

    @Override
    public ProviderProfileResponse updateProviderProfile(UpdateProviderProfileRequest request) {
        return null;
    }



    private ProviderProfile getAuthenticatedProvider() {

        User user = userService.getAuthenticatedUser();

        return providerProfileRepository.findByUser(user)
                .orElseThrow(() ->
                        new ProviderNotFoundException("Provider profile not found"));
    }

    @Override
    public ProviderProfileResponse getMyProviderProfile() {

        ProviderProfile provider = getAuthenticatedProvider();

        return providerMapper.toResponse(provider);
    }



    @Override
    @Transactional
    public UploadDocumentResponse uploadDocument(
            MultipartFile file,
            DocumentType documentType,
            String documentNumber) {

        ProviderProfile provider = getAuthenticatedProvider();

        // Check duplicate document
        if (providerDocumentRepository
                .findByProviderAndDocumentType(provider, documentType)
                .isPresent()) {

            throw new DuplicateDocumentException(
                    documentType + " document already uploaded."
            );
        }

        // Upload file
        String filePath = storageService.uploadFile(
                file,
                "providers/documents"
        );

        // Create entity
        ProviderDocument document = new ProviderDocument();

        document.setProvider(provider);
        document.setDocumentType(documentType);
        document.setDocumentNumber(documentNumber);
        document.setDocumentUrl(filePath);
        document.setVerified(false);

        ProviderDocument savedDocument =
                providerDocumentRepository.save(document);

        return providerDocumentMapper.toResponse(savedDocument);
    }



    @Override
    @Transactional
    public void saveOrUpdateAvailability(
            List<SaveAvailabilityRequest> requests) {

        ProviderProfile provider = getAuthenticatedProvider();

        for (SaveAvailabilityRequest request : requests) {

            ProviderAvailability availability =
                    providerAvailabilityRepository
                            .findByProviderAndDayOfWeek(
                                    provider,
                                    request.getDayOfWeek())
                            .orElse(new ProviderAvailability());

            availability.setProvider(provider);

            providerAvailabilityMapper.updateEntity(
                    request,
                    availability
            );

            if (!request.getAvailable()) {

                availability.setStartTime(null);
                availability.setEndTime(null);

            } else {

                if (request.getStartTime() == null ||
                        request.getEndTime() == null) {

                    throw new InvalidAvailabilityException(
                            "Start time and end time are required."
                    );
                }

                if (!request.getStartTime()
                        .isBefore(request.getEndTime())) {

                    throw new InvalidAvailabilityException(
                            "End time must be after start time."
                    );
                }
            }

            providerAvailabilityRepository.save(availability);
        }
    }


    @Override
    public List<AvailabilityResponse> getAvailability() {

        ProviderProfile provider = getAuthenticatedProvider();

        return providerAvailabilityRepository
                .findByProvider(provider)
                .stream()
                .map(providerAvailabilityMapper::toResponse)
                .toList();
    }

    @Override
    public Optional<ProviderAvailability> findByProviderAndDayOfWeek(ProviderProfile provider, DayOfWeek dayOfWeek) {
        return Optional.empty();
    }


    @Override

    public Optional<ProviderAvailability> getAvailabilityForDay(
            ProviderProfile provider,
            DayOfWeek dayOfWeek) {

        return providerAvailabilityRepository
                .findByProviderAndDayOfWeek(
                        provider,
                        dayOfWeek
                );
    }


}