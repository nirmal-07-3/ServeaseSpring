package com.application.Servease.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PlatformSettingsResponse {

    private BigDecimal platformFeeRate;

    private Long providerResponseTimeoutMinutes;

    private Long onlinePaymentTimeoutMinutes;

    private LocalDateTime updatedAt;
}