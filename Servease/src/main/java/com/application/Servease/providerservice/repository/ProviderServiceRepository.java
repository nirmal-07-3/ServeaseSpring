package com.application.Servease.providerservice.repository;

import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.provider.enums.VerificationStatus;
import com.application.Servease.providerservice.entity.ProviderService;
import com.application.Servease.subcategory.entity.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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


    @Query("""
    SELECT s
    FROM ProviderService s
    JOIN s.provider p
    JOIN s.subCategory sc
    JOIN sc.category c
    WHERE s.active = true
      AND p.verificationStatus = :approvedStatus
      AND (:categoryId IS NULL OR c.id = :categoryId)
      AND (:subCategoryId IS NULL OR sc.id = :subCategoryId)
      AND (:minPrice IS NULL OR s.basePrice >= :minPrice)
      AND (:maxPrice IS NULL OR s.basePrice <= :maxPrice)
      AND (
          :keyword IS NULL
          OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(COALESCE(s.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(p.businessName) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(sc.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
    ORDER BY s.createdAt DESC
    """)
    List<ProviderService> searchActiveServices(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("subCategoryId") Long subCategoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("approvedStatus") VerificationStatus approvedStatus
    );
}