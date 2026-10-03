package com.application.Servease.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RazorpayOrderResponse {

    private Long paymentId;

    private Long bookingId;

    private String razorpayOrderId;

    private String razorpayKeyId;

    private BigDecimal amount;

    private String currency;

    private String status;
}