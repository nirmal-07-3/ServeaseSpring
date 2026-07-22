package com.application.Servease.auth.controller;

import com.application.Servease.auth.dto.*;
import com.application.Servease.auth.service.AuthService;
import com.application.Servease.common.dto.ApiResponse;
import com.application.Servease.common.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    public AuthController(AuthService authService, EmailService emailService) {
        this.authService = authService;
        this.emailService = emailService;
    }


    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @RequestBody RegisterRequest request){

        authService.register(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "User registered successfully",
                        null
                )
        );
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Login successful",
                        response
                )
        );
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "OTP sent successfully to your email.",
                        null
                )
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        authService.verifyOtp(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "OTP verified successfully.",
                        null
                )
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Password reset successfully.",
                        null
                )
        );
    }




}