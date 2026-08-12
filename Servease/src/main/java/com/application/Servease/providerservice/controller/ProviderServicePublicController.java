package com.application.Servease.providerservice.controller;

import com.application.Servease.common.dto.ApiResponse;
import com.application.Servease.providerservice.dto.ProviderServiceResponse;
import com.application.Servease.providerservice.service.ProviderServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subcategories")
@RequiredArgsConstructor
@Tag(
        name = "Public Provider Services",
        description = "Public APIs for browsing provider services"
)
public class ProviderServicePublicController {

    private final ProviderServiceService providerServiceService;

    @Operation(
            summary = "Get services by subcategory",
            description = "Returns all active provider services under a subcategory"
    )
    @GetMapping("/{subCategoryId}/services")
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