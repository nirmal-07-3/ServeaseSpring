
package com.application.Servease.common.service.impl;

import com.application.Servease.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Servease Password Reset OTP");
        message.setText("""
                Hello,

                Your OTP for password reset is:

                %s

                This OTP is valid for 5 minutes.

                If you did not request this password reset, please ignore this email.

                Regards,
                Servease Team
                """.formatted(otp));

        mailSender.send(message);
    }

    @Override
    public void sendBookingVerificationOtpEmail(
            String toEmail,
            String otp,
            Long bookingId,
            String purpose
    ) {
        String action = "START".equals(purpose)
                ? "service start"
                : "service completion";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Servease Booking Verification Code");
        message.setText("""
                Hello,

                A verification code was requested for booking #%d.

                Action: %s

                Your verification code is:

                %s

                This code expires in 5 minutes and can be used only once.

                Do not share this code with anyone other than your assigned
                Servease service provider for the intended verification.

                Regards,
                Servease Team
                """.formatted(bookingId, action, otp));

        mailSender.send(message);
    }
}
