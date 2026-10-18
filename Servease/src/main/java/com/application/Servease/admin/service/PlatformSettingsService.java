package com.application.Servease.admin.service;

import com.application.Servease.admin.dto.PlatformSettingsRequest;
import com.application.Servease.admin.dto.PlatformSettingsResponse;
import com.application.Servease.admin.entity.PlatformSettings;
import com.application.Servease.admin.repository.PlatformSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlatformSettingsService {

    private final PlatformSettingsRepository repository;

    @Value("${servease.platform-fee-rate:10.00}")
    private BigDecimal defaultPlatformFeeRate;

    @Value("${servease.booking.provider-response-timeout-minutes:15}")
    private long defaultProviderResponseTimeoutMinutes;

    @Value("${servease.booking.online-payment-timeout-minutes:30}")
    private long defaultOnlinePaymentTimeoutMinutes;

    @Transactional
    public PlatformSettingsResponse getSettings() {
        return toResponse(getOrCreateSettings());
    }

    @Transactional
    public PlatformSettingsResponse updateSettings(
            PlatformSettingsRequest request) {

        if (request.getPlatformFeeRate() == null
                || request.getPlatformFeeRate().compareTo(BigDecimal.ZERO) < 0
                || request.getPlatformFeeRate()
                .compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException(
                    "Platform fee must be between 0 and 100."
            );
        }

        if (request.getProviderResponseTimeoutMinutes() == null
                || request.getProviderResponseTimeoutMinutes() <= 0
                || request.getProviderResponseTimeoutMinutes() > 10080) {
            throw new IllegalArgumentException(
                    "Provider response timeout must be between 1 and 10080 minutes."
            );
        }

        if (request.getOnlinePaymentTimeoutMinutes() == null
                || request.getOnlinePaymentTimeoutMinutes() <= 0
                || request.getOnlinePaymentTimeoutMinutes() > 10080) {
            throw new IllegalArgumentException(
                    "Online payment timeout must be between 1 and 10080 minutes."
            );
        }

        PlatformSettings settings = getOrCreateSettings();

        settings.setPlatformFeeRate(
                request.getPlatformFeeRate().setScale(
                        2,
                        java.math.RoundingMode.HALF_UP
                )
        );

        settings.setProviderResponseTimeoutMinutes(
                request.getProviderResponseTimeoutMinutes()
        );

        settings.setOnlinePaymentTimeoutMinutes(
                request.getOnlinePaymentTimeoutMinutes()
        );

        settings.setUpdatedAt(LocalDateTime.now());

        return toResponse(repository.save(settings));
    }

    private PlatformSettings getOrCreateSettings() {
        return repository.findById(1L).orElseGet(() -> {
            PlatformSettings settings = new PlatformSettings();
            settings.setId(1L);
            settings.setPlatformFeeRate(defaultPlatformFeeRate);
            settings.setProviderResponseTimeoutMinutes(
                    defaultProviderResponseTimeoutMinutes
            );
            settings.setOnlinePaymentTimeoutMinutes(
                    defaultOnlinePaymentTimeoutMinutes
            );
            settings.setUpdatedAt(LocalDateTime.now());

            return repository.save(settings);
        });
    }

    private PlatformSettingsResponse toResponse(
            PlatformSettings settings) {

        return new PlatformSettingsResponse(
                settings.getPlatformFeeRate(),
                settings.getProviderResponseTimeoutMinutes(),
                settings.getOnlinePaymentTimeoutMinutes(),
                settings.getUpdatedAt()
        );
    }
}