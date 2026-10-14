package com.application.Servease.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "razorpay_webhook_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UKrbyohd2d8uxmyy78cnppgmb5c",
                        columnNames = {"event_id"}
                )
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

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @Builder.Default
    @Column(name = "processing_status", nullable = false, length = 30)
    private String processingStatus = "RECEIVED";

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;
}