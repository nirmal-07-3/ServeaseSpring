package com.application.Servease.common.service.impl;

import com.application.Servease.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String toEmail, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom("servease");
        message.setTo(toEmail);

        message.setSubject("Servease Password Reset OTP");

        message.setText("""
                Hello,(

                Your OTP for password reset is:

                %s

                This OTP is valid for 5 minutes.

                If you didn't request this password reset, please ignore this email.

                Regards,
                Servease Team
                """.formatted(otp));


            mailSender.send(message);

    }
}