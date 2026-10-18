package com.application.Servease.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PlatformSettingsRequest {

    private BigDecimal platformFeeRate;

    private Long providerResponseTimeoutMinutes;

    private Long onlinePaymentTimeoutMinutes;
}