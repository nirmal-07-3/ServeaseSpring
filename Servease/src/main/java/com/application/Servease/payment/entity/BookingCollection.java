
package com.application.Servease.payment.entity;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.payment.enums.CollectionMethod;
import com.application.Servease.payment.enums.CollectionStatus;
import com.application.Servease.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "booking_collections",
        indexes = {
                @Index(
                        name = "idx_booking_collection_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "collection_method", nullable = false, length = 20)
    private CollectionMethod collectionMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CollectionStatus status = CollectionStatus.PENDING;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_attempt_id", unique = true)
    private PaymentAttempt paymentAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_user_id")
    private User confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
