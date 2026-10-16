
package com.application.Servease.providerservice.controller;

import com.application.Servease.common.dto.ApiResponse;
import com.application.Servease.providerservice.dto.ProviderServiceResponse;
import com.application.Servease.providerservice.service.ProviderServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(
        name = "Public Provider Services",
        description = "Public APIs for browsing and searching provider services"
)
public class ProviderServicePublicController {

    private final ProviderServiceService providerServiceService;

    @Operation(
            summary = "Search active services",
            description = "Search and filter services by text, category, subcategory and price"
    )
    @GetMapping("/services/search")
    public ResponseEntity<ApiResponse<List<ProviderServiceResponse>>> searchServices(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subCategoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "newest") String sort) {

        if (!List.of("newest", "priceLow", "priceHigh").contains(sort)) {
            throw new IllegalArgumentException(
                    "Sort must be newest, priceLow, or priceHigh."
            );
        }

        List<ProviderServiceResponse> services =
                providerServiceService.searchServices(
                        q,
                        categoryId,
                        subCategoryId,
                        minPrice,
                        maxPrice,
                        sort
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Services searched successfully.",
                        services
                )
        );
    }

    @Operation(
            summary = "Get services by subcategory",
            description = "Returns active provider services under a subcategory"
    )
    @GetMapping("/subcategories/{subCategoryId}/services")
    public ResponseEntity<ApiResponse<List<ProviderServiceResponse>>> getServicesBySubCategory(
            @PathVariable Long subCategoryId) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Services fetched successfully.",
                        providerServiceService.getServicesBySubCategory(
                                subCategoryId
                        )
                )
        );
    }
}
