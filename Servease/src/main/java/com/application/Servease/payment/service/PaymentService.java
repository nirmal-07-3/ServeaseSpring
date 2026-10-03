package com.application.Servease.payment.service;

import com.application.Servease.payment.dto.CreatePaymentRequest;
import com.application.Servease.payment.dto.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse createPayment(
            CreatePaymentRequest request
    );

    PaymentResponse getPaymentByBooking(
            Long bookingId
    );

    List<PaymentResponse> getMyPayments();
}