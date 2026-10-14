package com.application.Servease.payment.controller;

import com.application.Servease.common.dto.ApiResponse;
import com.application.Servease.payment.dto.BookingVerificationRequest;
import com.application.Servease.payment.enums.VerificationPurpose;
import com.application.Servease.payment.service.BookingVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/provider/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
public class BookingVerificationController {

    private final BookingVerificationService verificationService;

    @PostMapping("/{bookingId}/verification/{purpose}/request")
    public ResponseEntity<ApiResponse<Void>> requestCode(
            @PathVariable Long bookingId,
            @PathVariable VerificationPurpose purpose
    ) {
        verificationService.requestCode(bookingId, purpose);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Verification code sent to the customer's registered email.",
                        null
                )
        );
    }

    @PostMapping("/{bookingId}/verification/{purpose}/verify")
    public ResponseEntity<ApiResponse<Void>> verifyCode(
            @PathVariable Long bookingId,
            @PathVariable VerificationPurpose purpose,
            @Valid @RequestBody BookingVerificationRequest request
    ) {
        verificationService.verifyCode(bookingId, purpose, request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        purpose == VerificationPurpose.START
                                ? "Service start verified."
                                : "Service completion verified.",
                        null
                )
        );
    }
}