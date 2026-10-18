package com.application.Servease.admin.dto;

import java.math.BigDecimal;

public record AdminAnalyticsResponse(
        long totalUsers,
        long totalProviders,
        long totalBookings,
        BigDecimal platformRevenue,
        long pendingProviderVerifications
) {
}