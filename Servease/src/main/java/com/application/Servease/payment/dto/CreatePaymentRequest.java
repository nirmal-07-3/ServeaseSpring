package com.application.Servease.payment.dto;

import com.application.Servease.payment.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePaymentRequest {

    @NotNull(message = "Booking id is required")
    private Long bookingId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}