package com.application.Servease.provider.controller;

import com.application.Servease.common.dto.ApiResponse;
import com.application.Servease.provider.dto.ProviderApprovalResponse;
import com.application.Servease.provider.service.ProviderAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/providers")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Providers",
        description = "Admin provider approval and management APIs"
)
public class ProviderAdminController {

    private final ProviderAdminService providerAdminService;

    @Operation(
            summary = "Get pending providers",
            description = "Returns all providers waiting for admin approval"
    )
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ProviderApprovalResponse>>>
    getPendingProviders() {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Pending providers fetched successfully.",
                        providerAdminService.getPendingProviders()
                )
        );
    }

    @Operation(
            summary = "Approve provider",
            description = "Approves a provider registration"
    )
    @PatchMapping("/{providerId}/approve")
    public ResponseEntity<ApiResponse<ProviderApprovalResponse>>
    approveProvider(
            @PathVariable Long providerId) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Provider approved successfully.",
                        providerAdminService.approveProvider(providerId)
                )
        );
    }

    @Operation(
            summary = "Reject provider",
            description = "Rejects a provider registration"
    )
    @PatchMapping("/{providerId}/reject")
    public ResponseEntity<ApiResponse<ProviderApprovalResponse>>
    rejectProvider(
            @PathVariable Long providerId) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Provider rejected successfully.",
                        providerAdminService.rejectProvider(providerId)
                )
        );
    }
}