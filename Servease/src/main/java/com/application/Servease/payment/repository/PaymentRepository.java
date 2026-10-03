package com.application.Servease.payment.repository;

import com.application.Servease.payment.entity.Payment;
import com.application.Servease.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    Optional<Payment> findByTransactionId(
            String transactionId
    );

    List<Payment> findByStatusOrderByCreatedAtDesc(
            PaymentStatus status
    );

    Optional<Payment> findByGatewayOrderId(
            String gatewayOrderId
    );
}