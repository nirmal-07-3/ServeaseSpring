package com.application.Servease.payment.dto;

import lombok.Data;

@Data
public class RazorpayWebhookRequest {

    private String event;

    private String payload;
}