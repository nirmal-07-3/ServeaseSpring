package com.application.Servease.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "razorpay_webhook_events",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "event_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RazorpayWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "event_id",
            nullable = false,
            unique = true
    )
    private String eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private LocalDateTime receivedAt;
}