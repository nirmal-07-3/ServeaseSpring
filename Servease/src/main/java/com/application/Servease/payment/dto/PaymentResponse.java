package com.application.Servease.payment.dto;

import com.application.Servease.payment.enums.PaymentMethod;
import com.application.Servease.payment.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {

    private Long id;

    private Long bookingId;

    private BigDecimal amount;

    private PaymentStatus status;

    private PaymentMethod paymentMethod;

    private String transactionId;

    private String gatewayOrderId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}