package com.application.Servease.payment.enums;

public enum PaymentAttemptStatus {
    CREATED,
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    CANCELLED,
    REFUND_PENDING,
    REFUNDED,
    RECONCILIATION_REQUIRED
}