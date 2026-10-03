package com.application.Servease.payment.mapper;

import com.application.Servease.payment.dto.PaymentResponse;
import com.application.Servease.payment.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {

        return PaymentResponse.builder()
                .id(payment.getId())

                .bookingId(
                        payment.getBooking().getId()
                )

                .amount(payment.getAmount())

                .status(payment.getStatus())

                .paymentMethod(
                        payment.getPaymentMethod()
                )

                .transactionId(
                        payment.getTransactionId()
                )

                .gatewayOrderId(
                        payment.getGatewayOrderId()
                )

                .createdAt(payment.getCreatedAt())

                .updatedAt(payment.getUpdatedAt())

                .build();
    }
}