package com.application.Servease.payment.entity;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.payment.enums.PaymentMethod;
import com.application.Servease.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = "booking_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * One booking should have one payment record.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "booking_id",
            nullable = false
    )
    private Booking booking;

    /*
     * Amount that the customer is expected to pay.
     *
     * This is copied from Booking.agreedPrice.
     */
    @Column(
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    /*
     * ID returned by the payment gateway.
     *
     * For example, Razorpay payment/order ID later.
     */
    @Column(unique = true)
    private String transactionId;

    /*
     * Optional gateway order ID.
     *
     * Useful when integrating Razorpay later.
     */
    private String gatewayOrderId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}