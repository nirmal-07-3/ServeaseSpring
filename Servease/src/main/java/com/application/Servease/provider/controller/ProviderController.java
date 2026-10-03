package com.application.Servease.provider.controller;


import com.application.Servease.common.dto.ApiResponse;
import com.application.Servease.provider.dto.*;
import com.application.Servease.provider.enums.DocumentType;
import com.application.Servease.provider.service.ProviderService;
import com.application.Servease.user.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Provider", description = "Provider Management APIs")
@RestController
    @RequestMapping("/api/providers")
    @RequiredArgsConstructor
    public class ProviderController {

        private final ProviderService providerService;



    @PostMapping("/register")
        public ResponseEntity<ApiResponse<ProviderProfileResponse>> becomeProvider(
                @Valid @RequestBody BecomeProviderRequest request) {

            ProviderProfileResponse response = providerService.becomeProvider(request);

            return ResponseEntity.ok(
                    new ApiResponse<>(
                            true,
                            "Provider registration submitted successfully. Waiting for admin approval.",
                            response
                    )
            );
        }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProviderProfileResponse>> getMyProviderProfile() {

        ProviderProfileResponse response = providerService.getMyProviderProfile();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Provider profile fetched successfully.",
                        response
                )
        );
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<ProviderProfileResponse>>
    updateProviderProfile(
            @Valid @RequestBody UpdateProviderProfileRequest request) {

        ProviderProfileResponse response =
                providerService.updateProviderProfile(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Provider profile updated successfully.",
                        response
                )
        );
    }


    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<UploadDocumentResponse>> uploadDocument(

            @RequestParam("file") MultipartFile file,

            @RequestParam("documentType") DocumentType documentType,

            @RequestParam("documentNumber") String documentNumber
    ) {

        UploadDocumentResponse response =
                providerService.uploadDocument(
                        file,
                        documentType,
                        documentNumber
                );

        return ResponseEntity.ok(

                new ApiResponse<>(

                        true,

                        "Document uploaded successfully.",

                        response
                )
        );
    }


    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<UploadDocumentResponse>>>
    getMyDocuments() {

        List<UploadDocumentResponse> response =
                providerService.getMyDocuments();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Provider documents fetched successfully.",
                        response
                )
        );
    }

    @PutMapping("/availability")
    public ResponseEntity<ApiResponse<Void>>
    saveOrUpdateAvailability(
            @Valid @RequestBody
            List<SaveAvailabilityRequest> request) {

        providerService.saveOrUpdateAvailability(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Availability updated successfully.",
                        null
                )
        );
    }

    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<List<AvailabilityResponse>>>
    getAvailability() {

        List<AvailabilityResponse> response =
                providerService.getAvailability();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Availability fetched successfully.",
                        response
                )
        );
    }
    }




