
package com.application.Servease.payment.controller;

import com.application.Servease.common.dto.ApiResponse;
import com.application.Servease.payment.dto.PaymentResponse;
import com.application.Servease.payment.service.RazorpayReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/payments/razorpay")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(
        name = "Admin - Payment Reconciliation",
        description = "Safely reconcile an existing Razorpay payment"
)
public class AdminPaymentReconciliationController {

    private final RazorpayReconciliationService reconciliationService;

    @PostMapping("/reconcile/booking/{bookingId}")
    @Operation(
            summary = "Reconcile an existing Razorpay order",
            description = "Verifies the existing gateway order and captured payment "
                    + "before updating local payment records."
    )
    public ResponseEntity<ApiResponse<PaymentResponse>> reconcileBooking(
            @PathVariable Long bookingId) {

        PaymentResponse response =
                reconciliationService.reconcileExistingOrder(bookingId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Existing Razorpay payment reconciled successfully.",
                        response
                )
        );
    }
}
