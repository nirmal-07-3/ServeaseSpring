
package com.application.Servease.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingOnlineQrResponse {
    private Long bookingId;
    private Long collectionId;
    private BigDecimal amount;
    private String currency;
    private String qrCodeId;
    private String gatewayPaymentId;
    private String imageUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
