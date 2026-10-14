package com.application.Servease.payment.entity;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.payment.enums.VerificationPurpose;
import com.application.Servease.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "booking_verifications",
        indexes = {
                @Index(
                        name = "idx_booking_verification_lookup",
                        columnList = "booking_id,purpose,created_at"
                ),
                @Index(
                        name = "idx_booking_verification_expiry",
                        columnList = "expires_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationPurpose purpose;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "attempts_used", nullable = false)
    private Integer attemptsUsed = 0;

    @Builder.Default
    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 5;

    @Column(name = "resend_available_at")
    private LocalDateTime resendAvailableAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "invalidated_at")
    private LocalDateTime invalidatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}