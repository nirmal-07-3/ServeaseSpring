package com.application.Servease.provider.service;

import com.application.Servease.common.exception.ProviderNotFoundException;
import com.application.Servease.provider.dto.ProviderAdminDetailsResponse;
import com.application.Servease.provider.dto.ProviderApprovalResponse;
import com.application.Servease.provider.dto.UploadDocumentResponse;
import com.application.Servease.provider.entity.ProviderDocument;
import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.provider.enums.VerificationStatus;
import com.application.Servease.provider.mapper.ProviderDocumentMapper;
import com.application.Servease.provider.repository.ProviderDocumentRepository;
import com.application.Servease.provider.repository.ProviderProfileRepository;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.enums.UserStatus;
import com.application.Servease.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProviderAdminServiceImpl
        implements ProviderAdminService {

    private final ProviderProfileRepository providerProfileRepository;

    private final ProviderDocumentRepository providerDocumentRepository;

    private final ProviderDocumentMapper providerDocumentMapper;

    private final UserRepository userRepository;


    // ============================================================
    // GET PENDING PROVIDERS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<ProviderApprovalResponse> getPendingProviders() {

        return providerProfileRepository
                .findByVerificationStatus(
                        VerificationStatus.PENDING
                )
                .stream()
                .map(this::toApprovalResponse)
                .toList();
    }


    // ============================================================
    // GET PROVIDER DETAILS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public ProviderAdminDetailsResponse getProviderDetails(
            Long providerId) {

        ProviderProfile provider =
                getProvider(providerId);

        List<UploadDocumentResponse> documents =
                providerDocumentRepository
                        .findByProvider(provider)
                        .stream()
                        .map(providerDocumentMapper::toResponse)
                        .toList();

        return ProviderAdminDetailsResponse.builder()

                .id(provider.getId())

                .businessName(
                        provider.getBusinessName()
                )

                .description(
                        provider.getDescription()
                )

                .city(
                        provider.getCity()
                )

                .state(
                        provider.getState()
                )

                .country(
                        provider.getCountry()
                )

                .pincode(
                        provider.getPincode()
                )

                .experienceYears(
                        provider.getExperienceYears()
                )

                .latitude(
                        provider.getLatitude()
                )

                .longitude(
                        provider.getLongitude()
                )

                .profileCompleted(
                        provider.getProfileCompleted()
                )

                .averageRating(
                        provider.getAverageRating()
                )

                .totalReviews(
                        provider.getTotalReviews()
                )

                .totalBookings(
                        provider.getTotalBookings()
                )

                .verificationStatus(
                        provider.getVerificationStatus()
                                != null
                                ? provider.getVerificationStatus()
                                .name()
                                : null
                )

                .documents(documents)

                .build();
    }


    // ============================================================
    // VERIFY DOCUMENT
    // ============================================================

    @Override
    public UploadDocumentResponse verifyDocument(
            Long documentId) {

        ProviderDocument document =
                providerDocumentRepository
                        .findById(documentId)
                        .orElseThrow(() ->
                                new ProviderNotFoundException(
                                        "Provider document not found."
                                )
                        );

        // Mark the document as verified.
        document.setVerified(true);

        ProviderDocument savedDocument =
                providerDocumentRepository.save(
                        document
                );

        return providerDocumentMapper.toResponse(
                savedDocument
        );
    }


    // ============================================================
    // APPROVE PROVIDER
    // ============================================================

    @Override
    public ProviderApprovalResponse approveProvider(
            Long providerId) {

        ProviderProfile provider =
                getProvider(providerId);

        provider.setVerificationStatus(
                VerificationStatus.APPROVED
        );

        User user =
                provider.getUser();

        user.setStatus(
                UserStatus.ACTIVE
        );

        userRepository.save(user);

        ProviderProfile saved =
                providerProfileRepository.save(
                        provider
                );

        return toApprovalResponse(saved);
    }


    // ============================================================
    // REJECT PROVIDER
    // ============================================================

    @Override
    public ProviderApprovalResponse rejectProvider(
            Long providerId) {

        ProviderProfile provider =
                getProvider(providerId);

        provider.setVerificationStatus(
                VerificationStatus.REJECTED
        );

        User user =
                provider.getUser();

        user.setStatus(
                UserStatus.BLOCKED
        );

        userRepository.save(user);

        ProviderProfile saved =
                providerProfileRepository.save(
                        provider
                );

        return toApprovalResponse(saved);
    }


    // ============================================================
    // FIND PROVIDER
    // ============================================================

    private ProviderProfile getProvider(
            Long providerId) {

        return providerProfileRepository
                .findById(providerId)
                .orElseThrow(() ->
                        new ProviderNotFoundException(
                                "Provider not found."
                        )
                );
    }


    // ============================================================
    // APPROVAL RESPONSE MAPPER
    // ============================================================

    private ProviderApprovalResponse toApprovalResponse(
            ProviderProfile provider) {

        return ProviderApprovalResponse.builder()

                .id(
                        provider.getId()
                )

                .businessName(
                        provider.getBusinessName()
                )

                .description(
                        provider.getDescription()
                )

                .city(
                        provider.getCity()
                )

                .state(
                        provider.getState()
                )

                .country(
                        provider.getCountry()
                )

                .pincode(
                        provider.getPincode()
                )

                .experienceYears(
                        provider.getExperienceYears()
                )

                .verificationStatus(
                        provider.getVerificationStatus()
                )

                .profileCompleted(
                        provider.getProfileCompleted()
                )

                .build();
    }
}