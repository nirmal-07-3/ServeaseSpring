package com.application.Servease.payment.repository;

import com.application.Servease.payment.entity.RazorpayWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RazorpayWebhookEventRepository
        extends JpaRepository<RazorpayWebhookEvent, Long> {

    boolean existsByEventId(String eventId);
}