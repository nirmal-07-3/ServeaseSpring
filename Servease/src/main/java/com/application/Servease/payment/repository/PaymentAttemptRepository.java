package com.application.Servease.payment.repository;

import com.application.Servease.payment.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    List<PaymentAttempt> findByBookingIdOrderByAttemptNumberDesc(Long bookingId);

    Optional<PaymentAttempt> findByGatewayOrderId(String gatewayOrderId);

    Optional<PaymentAttempt> findByGatewayPaymentId(String gatewayPaymentId);

    Optional<PaymentAttempt> findByIdempotencyKey(String idempotencyKey);

    boolean existsByBookingIdAndAttemptNumber(Long bookingId, Integer attemptNumber);
}