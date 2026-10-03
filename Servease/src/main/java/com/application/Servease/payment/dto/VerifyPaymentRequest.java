package com.application.Servease.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyPaymentRequest {

    @NotBlank(message = "Payment ID is required")
    private String razorpayPaymentId;

    @NotBlank(message = "Order ID is required")
    private String razorpayOrderId;

    @NotBlank(message = "Signature is required")
    private String razorpaySignature;
}