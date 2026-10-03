package com.application.Servease.payment.service;

import com.application.Servease.payment.dto.PaymentResponse;
import com.application.Servease.payment.dto.RazorpayOrderResponse;
import com.application.Servease.payment.dto.VerifyPaymentRequest;

public interface RazorpayPaymentService {

    RazorpayOrderResponse createOrder(Long bookingId);

    PaymentResponse verifyPayment(
            VerifyPaymentRequest request
    );
}