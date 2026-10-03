package com.application.Servease.common.exception;

public class PaymentConflictException extends RuntimeException {
    public PaymentConflictException(String message) {
        super(message);
    }
}
