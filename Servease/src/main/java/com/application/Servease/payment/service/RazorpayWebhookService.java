package com.application.Servease.payment.service;

public interface RazorpayWebhookService {

    void processWebhook(
            String payload,
            String signature,
            String eventId
    );
}