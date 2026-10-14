
package com.application.Servease.common.service;

public interface EmailService {

    void sendOtpEmail(String toEmail, String otp);

    void sendBookingVerificationOtpEmail(
            String toEmail,
            String otp,
            Long bookingId,
            String purpose
    );
}
