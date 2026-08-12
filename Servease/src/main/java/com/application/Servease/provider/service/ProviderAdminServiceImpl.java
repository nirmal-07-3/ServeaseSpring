package com.application.Servease.provider.service;

import com.application.Servease.common.exception.ProviderNotFoundException;
import com.application.Servease.provider.dto.ProviderApprovalResponse;
import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.provider.enums.VerificationStatus;
import com.application.Servease.provider.mapper.ProviderMapper;
import com.application.Servease.provider.repository.ProviderProfileRepository;
import com.application.Servease.provider.service.ProviderAdminService;
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

    private final ProviderMapper providerMapper;

    private final UserRepository userRepository;

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

    @Override
    public ProviderApprovalResponse approveProvider(
            Long providerId) {

        ProviderProfile provider =
                getProvider(providerId);

        provider.setVerificationStatus(
                VerificationStatus.APPROVED
        );

        User user = provider.getUser();

        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);

        ProviderProfile saved =
                providerProfileRepository.save(provider);

        return toApprovalResponse(saved);
    }

    @Override
    public ProviderApprovalResponse rejectProvider(
            Long providerId) {

        ProviderProfile provider =
                getProvider(providerId);

        provider.setVerificationStatus(
                VerificationStatus.REJECTED
        );

        User user = provider.getUser();

        user.setStatus(UserStatus.BLOCKED);

        userRepository.save(user);

        ProviderProfile saved =
                providerProfileRepository.save(provider);

        return toApprovalResponse(saved);
    }

    private ProviderProfile getProvider(Long providerId) {

        return providerProfileRepository
                .findById(providerId)
                .orElseThrow(() ->
                        new ProviderNotFoundException(
                                "Provider not found."
                        ));
    }

    private ProviderApprovalResponse toApprovalResponse(
            ProviderProfile provider) {

        return ProviderApprovalResponse.builder()
                .id(provider.getId())
                .businessName(provider.getBusinessName())
                .description(provider.getDescription())
                .city(provider.getCity())
                .state(provider.getState())
                .country(provider.getCountry())
                .pincode(provider.getPincode())
                .experienceYears(provider.getExperienceYears())
                .verificationStatus(
                        provider.getVerificationStatus()
                )
                .profileCompleted(
                        provider.getProfileCompleted()
                )
                .build();
    }
}