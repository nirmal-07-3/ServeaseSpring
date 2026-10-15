package com.application.Servease.payment.controller;

import com.application.Servease.common.dto.ApiResponse;
import com.application.Servease.payment.dto.CreatePaymentRequest;
import com.application.Servease.payment.dto.PaymentResponse;
import com.application.Servease.payment.dto.RazorpayOrderResponse;
import com.application.Servease.payment.dto.VerifyPaymentRequest;
import com.application.Servease.payment.service.PaymentService;
import com.application.Servease.payment.service.RazorpayPaymentService;
import com.application.Servease.payment.service.RazorpayWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(
        name = "Customer - Payments",
        description = "Customer payment APIs"
)
public class PaymentController {

    private final PaymentService paymentService;

    private final RazorpayPaymentService razorpayPaymentService;

    private final RazorpayWebhookService razorpayWebhookService;


    // =====================================================
    // CREATE PAYMENT
    // =====================================================

    @Operation(
            summary = "Create payment",
            description = "Create a pending payment for an accepted booking"
    )
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Payment created successfully.",
                        paymentService.createPayment(request)
                )
        );
    }


    // =====================================================
    // GET PAYMENT BY BOOKING
    // =====================================================

    @Operation(
            summary = "Get payment by booking"
    )
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<PaymentResponse>>
    getPaymentByBooking(
            @PathVariable Long bookingId) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Payment fetched successfully.",
                        paymentService.getPaymentByBooking(
                                bookingId
                        )
                )
        );
    }


    // =====================================================
    // GET MY PAYMENTS
    // =====================================================

    @Operation(
            summary = "Get my payments"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponse>>>
    getMyPayments() {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Payments fetched successfully.",
                        paymentService.getMyPayments()
                )
        );
    }


    // =====================================================
    // CREATE RAZORPAY ORDER
    // =====================================================

    @Operation(
            summary = "Create Razorpay order",
            description = "Creates a Razorpay order for an accepted booking"
    )
    @PostMapping("/razorpay/order/{bookingId}")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>>
    createRazorpayOrder(
            @PathVariable Long bookingId) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Razorpay order created successfully.",
                        razorpayPaymentService.createOrder(
                                bookingId
                        )
                )
        );
    }


    // =====================================================
    // VERIFY RAZORPAY PAYMENT
    // =====================================================

    @Operation(
            summary = "Verify Razorpay payment",
            description = "Verifies Razorpay signature and marks payment as successful"
    )
    @PostMapping("/razorpay/verify")
    public ResponseEntity<ApiResponse<PaymentResponse>>
    verifyRazorpayPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Payment verified successfully.",
                        razorpayPaymentService.verifyPayment(
                                request
                        )
                )
        );
    }


    // =====================================================
    // RAZORPAY WEBHOOK
    // =====================================================

    @PostMapping("/razorpay/webhook")
    public ResponseEntity<String> razorpayWebhook(

            // Raw Razorpay webhook JSON
            @RequestBody(required = false)
            String payload,

            // Razorpay webhook signature
            @RequestHeader(
                    value = "X-Razorpay-Signature",
                    required = false
            )
            String signature,

            // Unique Razorpay webhook event ID
            @RequestHeader(
                    value = "x-razorpay-event-id",
                    required = false
            )
            String eventId
    ) {

        // -------------------------------------------------
        // Check payload
        // -------------------------------------------------

        if (payload == null || payload.isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body("Missing webhook payload.");
        }


        // -------------------------------------------------
        // Check signature
        // -------------------------------------------------

        if (signature == null || signature.isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body("Missing Razorpay webhook signature.");
        }


        // -------------------------------------------------
        // Check event ID
        // -------------------------------------------------

        if (eventId == null || eventId.isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body("Missing Razorpay event ID.");
        }


        // -------------------------------------------------
        // Process webhook
        // -------------------------------------------------

        razorpayWebhookService.processWebhook(
                payload,
                signature,
                eventId
        );


        // -------------------------------------------------
        // Success response
        // -------------------------------------------------

        return ResponseEntity.ok(
                "Webhook received."
        );
    }
}