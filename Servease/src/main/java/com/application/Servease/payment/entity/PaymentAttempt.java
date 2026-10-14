package com.application.Servease.payment.entity;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.payment.enums.PaymentAttemptStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_attempts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_attempt_booking_number",
                        columnNames = {"booking_id", "attempt_number"}
                ),
                @UniqueConstraint(
                        name = "uk_payment_attempt_order",
                        columnNames = {"gateway_order_id"}
                ),
                @UniqueConstraint(
                        name = "uk_payment_attempt_gateway_payment",
                        columnNames = {"gateway_payment_id"}
                ),
                @UniqueConstraint(
                        name = "uk_payment_attempt_idempotency",
                        columnNames = {"idempotency_key"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_payment_attempt_booking_status",
                        columnList = "booking_id,status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String provider = "RAZORPAY";

    @Column(name = "gateway_order_id", unique = true)
    private String gatewayOrderId;

    @Column(name = "gateway_payment_id", unique = true)
    private String gatewayPaymentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private PaymentAttemptStatus status = PaymentAttemptStatus.CREATED;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;
}