package com.application.Servease.common.exception;

public class OtpRequestTooFrequentException extends RuntimeException{

    public OtpRequestTooFrequentException(String message) {
        super(message);
    }
}
