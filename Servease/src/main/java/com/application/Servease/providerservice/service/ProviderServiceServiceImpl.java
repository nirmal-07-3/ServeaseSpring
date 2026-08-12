package com.application.Servease.providerservice.service;

import com.application.Servease.common.exception.*;
import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.provider.enums.VerificationStatus;
import com.application.Servease.provider.repository.ProviderProfileRepository;
import com.application.Servease.subcategory.entity.SubCategory;
import com.application.Servease.subcategory.repository.SubCategoryRepository;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.service.UserService;
import com.application.Servease.providerservice.dto.CreateProviderServiceRequest;
import com.application.Servease.providerservice.dto.ProviderServiceResponse;
import com.application.Servease.providerservice.dto.UpdateProviderServiceRequest;
import com.application.Servease.providerservice.dto.UpdateProviderServiceStatusRequest;
import com.application.Servease.providerservice.entity.ProviderService;
import com.application.Servease.providerservice.mapper.ProviderServiceMapper;
import com.application.Servease.providerservice.repository.ProviderServiceRepository;
import com.application.Servease.providerservice.service.ProviderServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProviderServiceServiceImpl
        implements ProviderServiceService {

    private final ProviderServiceRepository providerServiceRepository;

    private final ProviderServiceMapper providerServiceMapper;

    private final ProviderProfileRepository providerProfileRepository;

    private final SubCategoryRepository subCategoryRepository;

    private final UserService userService;

    private ProviderProfile getAuthenticatedProvider() {

        User user = userService.getAuthenticatedUser();

        return providerProfileRepository.findByUser(user)
                .orElseThrow(() ->
                        new ProviderNotFoundException(
                                "Provider profile not found."
                        ));
    }

    private ProviderProfile getApprovedProvider() {

        ProviderProfile provider = getAuthenticatedProvider();

        if (provider.getVerificationStatus()
                != VerificationStatus.APPROVED) {

            throw new ProviderNotApprovedException(
                    "Your provider profile is not approved yet."
            );
        }

        return provider;
    }

    @Override
    public ProviderServiceResponse createService(
            CreateProviderServiceRequest request) {

        ProviderProfile provider = getApprovedProvider();

        SubCategory subCategory =
                subCategoryRepository
                        .findByIdAndActiveTrue(
                                request.getSubCategoryId()
                        )
                        .orElseThrow(() ->
                                new SubCategoryNotFoundException(
                                        "Active subcategory not found."
                                ));

        if (providerServiceRepository
                .existsByProviderAndSubCategoryAndTitleIgnoreCase(
                        provider,
                        subCategory,
                        request.getTitle()
                )) {

            throw new ProviderServiceAlreadyExistsException(
                    "You already have a service with this name under this subcategory."
            );
        }

        ProviderService providerService =
                providerServiceMapper.toEntity(
                        request,
                        provider,
                        subCategory
                );

        ProviderService saved =
                providerServiceRepository.save(providerService);

        return providerServiceMapper.toResponse(saved);
    }


    @Override
    public ProviderServiceResponse updateService(
            Long id,
            UpdateProviderServiceRequest request) {

        ProviderProfile provider = getApprovedProvider();

        ProviderService providerService =
                providerServiceRepository
                        .findByIdAndProvider(id, provider)
                        .orElseThrow(() ->
                                new ProviderServiceNotFoundException(
                                        "Provider service not found."
                                ));

        if (!providerService.getTitle()
                .equalsIgnoreCase(request.getTitle())
                &&
                providerServiceRepository
                        .existsByProviderAndSubCategoryAndTitleIgnoreCase(
                                provider,
                                providerService.getSubCategory(),
                                request.getTitle()
                        )) {

            throw new ProviderServiceAlreadyExistsException(
                    "You already have a service with this name under this subcategory."
            );
        }

        providerServiceMapper.updateEntity(
                request,
                providerService
        );

        ProviderService updated =
                providerServiceRepository.save(providerService);

        return providerServiceMapper.toResponse(updated);
    }


    @Override
    @Transactional(readOnly = true)
    public List<ProviderServiceResponse> getMyServices() {

        ProviderProfile provider = getAuthenticatedProvider();

        return providerServiceRepository
                .findByProviderOrderByCreatedAtDesc(provider)
                .stream()
                .map(providerServiceMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderServiceResponse getMyService(Long id) {

        ProviderProfile provider = getAuthenticatedProvider();

        ProviderService providerService =
                providerServiceRepository
                        .findByIdAndProvider(id, provider)
                        .orElseThrow(() ->
                                new ProviderServiceNotFoundException(
                                        "Provider service not found."
                                ));

        return providerServiceMapper.toResponse(providerService);
    }

    @Override
    public void updateServiceStatus(
            Long id,
            UpdateProviderServiceStatusRequest request) {

        ProviderProfile provider = getAuthenticatedProvider();

        ProviderService providerService =
                providerServiceRepository
                        .findByIdAndProvider(id, provider)
                        .orElseThrow(() ->
                                new ProviderServiceNotFoundException(
                                        "Provider service not found."
                                ));

        providerService.setActive(request.getActive());

        providerServiceRepository.save(providerService);
    }


    @Override
    public void deleteService(Long id) {

        ProviderProfile provider = getAuthenticatedProvider();

        ProviderService providerService =
                providerServiceRepository
                        .findByIdAndProvider(id, provider)
                        .orElseThrow(() ->
                                new ProviderServiceNotFoundException(
                                        "Provider service not found."
                                ));

        providerService.setActive(false);

        providerServiceRepository.save(providerService);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderServiceResponse> getServicesBySubCategory(
            Long subCategoryId) {

        SubCategory subCategory =
                subCategoryRepository
                        .findByIdAndActiveTrue(subCategoryId)
                        .orElseThrow(() ->
                                new SubCategoryNotFoundException(
                                        "Active subcategory not found."
                                ));

        return providerServiceRepository
                .findBySubCategoryAndActiveTrueOrderByCreatedAtDesc(
                        subCategory
                )
                .stream()
                .map(providerServiceMapper::toResponse)
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public ProviderService getServiceEntity(Long id) {

        return providerServiceRepository
                .findByIdAndActiveTrue(id)
                .orElseThrow(() ->
                        new ProviderServiceNotFoundException(
                                "Active provider service not found."
                        ));
    }
}