package com.application.Servease.providerservice.controller;

import com.application.Servease.common.dto.ApiResponse;
import com.application.Servease.providerservice.dto.CreateProviderServiceRequest;
import com.application.Servease.providerservice.dto.ProviderServiceResponse;
import com.application.Servease.providerservice.dto.UpdateProviderServiceRequest;
import com.application.Servease.providerservice.dto.UpdateProviderServiceStatusRequest;
import com.application.Servease.providerservice.service.ProviderServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provider/services")
@RequiredArgsConstructor
@Tag(
        name = "Provider Services",
        description = "Provider service management APIs"
)
public class ProviderServiceController {

    private final ProviderServiceService providerServiceService;

    @Operation(
            summary = "Create a service",
            description = "Create a new service under an active subcategory"
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ProviderServiceResponse>> createService(
            @Valid @RequestBody CreateProviderServiceRequest request) {

        ProviderServiceResponse response =
                providerServiceService.createService(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Provider service created successfully.",
                        response
                )
        );
    }

    @Operation(
            summary = "Get my services",
            description = "Get all services created by the authenticated provider"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProviderServiceResponse>>> getMyServices() {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Provider services fetched successfully.",
                        providerServiceService.getMyServices()
                )
        );
    }

    @Operation(
            summary = "Get my service by ID"
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProviderServiceResponse>> getMyService(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Provider service fetched successfully.",
                        providerServiceService.getMyService(id)
                )
        );
    }

    @Operation(
            summary = "Update my service"
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProviderServiceResponse>> updateService(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProviderServiceRequest request) {

        ProviderServiceResponse response =
                providerServiceService.updateService(id, request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Provider service updated successfully.",
                        response
                )
        );
    }

    @Operation(
            summary = "Update service status"
    )
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProviderServiceStatusRequest request) {

        providerServiceService.updateServiceStatus(id, request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Provider service status updated successfully.",
                        null
                )
        );
    }

    @Operation(
            summary = "Delete my service",
            description = "Permanently deletes the provider service"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteService(
            @PathVariable Long id) {

        providerServiceService.deleteService(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Provider service deleted successfully.",
                        null
                )
        );
    }
}