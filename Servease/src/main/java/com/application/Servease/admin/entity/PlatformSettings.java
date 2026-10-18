package com.application.Servease.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "platform_settings")
@Getter
@Setter
public class PlatformSettings {

    @Id
    private Long id;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal platformFeeRate;

    @Column(nullable = false)
    private Long providerResponseTimeoutMinutes;

    @Column(nullable = false)
    private Long onlinePaymentTimeoutMinutes;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}