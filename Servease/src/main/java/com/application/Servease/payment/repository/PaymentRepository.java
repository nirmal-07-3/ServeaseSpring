package com.application.Servease.payment.repository;

import com.application.Servease.payment.entity.Payment;
import com.application.Servease.payment.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Payment p
            where p.gatewayOrderId = :gatewayOrderId
            """)
    Optional<Payment> findByGatewayOrderIdForUpdate(
            @Param("gatewayOrderId") String gatewayOrderId);
}
