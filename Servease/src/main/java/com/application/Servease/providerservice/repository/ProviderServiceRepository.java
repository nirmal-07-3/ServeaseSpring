package com.application.Servease.providerservice.repository;

import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.providerservice.entity.ProviderService;
import com.application.Servease.subcategory.entity.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderServiceRepository
        extends JpaRepository<ProviderService, Long> {

    /*
     * Check whether the same provider already
     * has a service with the same title
     * under the same subcategory.
     */
    boolean existsByProviderAndSubCategoryAndTitleIgnoreCase(
            ProviderProfile provider,
            SubCategory subCategory,
            String title
    );

    /*
     * Find a specific service belonging to a provider.
     */
    Optional<ProviderService> findByIdAndProvider(
            Long id,
            ProviderProfile provider
    );

    /*
     * Get all services belonging to a provider.
     */
    List<ProviderService> findByProviderOrderByCreatedAtDesc(
            ProviderProfile provider
    );

    /*
     * Get active services belonging to a provider.
     */
    List<ProviderService> findByProviderAndActiveTrueOrderByCreatedAtDesc(
            ProviderProfile provider
    );

    /*
     * Get active services under a subcategory.
     * This will be useful for customers later.
     */
    List<ProviderService> findBySubCategoryAndActiveTrueOrderByCreatedAtDesc(
            SubCategory subCategory
    );

    /*
     * Find active service by ID.
     * Useful for customer-facing APIs.
     */
    Optional<ProviderService> findByIdAndActiveTrue(Long id);
}