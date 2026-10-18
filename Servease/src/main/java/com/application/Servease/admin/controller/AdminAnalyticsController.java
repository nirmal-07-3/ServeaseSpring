package com.application.Servease.admin.controller;

import com.application.Servease.admin.dto.AdminAnalyticsResponse;
import com.application.Servease.admin.service.AdminAnalyticsService;
import com.application.Servease.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AdminAnalyticsResponse>> getOverview() {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Admin analytics fetched successfully.",
                        adminAnalyticsService.getOverview()
                )
        );
    }
}