package com.application.Servease.payment.service;

import com.application.Servease.common.exception.PaymentConflictException;
import com.application.Servease.common.exception.PaymentNotFoundException;
import com.application.Servease.payment.dto.PaymentResponse;
import com.application.Servease.payment.entity.PaymentAttempt;
import com.application.Servease.payment.enums.PaymentAttemptStatus;
import com.application.Servease.payment.enums.PaymentStatus;
import com.application.Servease.payment.mapper.PaymentMapper;
import com.application.Servease.payment.repository.PaymentAttemptRepository;
import com.application.Servease.payment.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayReconciliationService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentMapper paymentMapper;

    @Transactional
    public PaymentResponse reconcileExistingOrder(Long bookingId) {
        com.application.Servease.payment.entity.Payment initialPayment =
                paymentRepository.findByBookingId(bookingId)
                        .orElseThrow(() -> new PaymentNotFoundException(
                                "No payment record exists for this booking."));

        String orderId = initialPayment.getGatewayOrderId();

        if (orderId == null || orderId.isBlank()) {
            throw new PaymentConflictException(
                    "No existing Razorpay order is recorded for this booking.");
        }

        com.application.Servease.payment.entity.Payment payment =
                paymentRepository.findByGatewayOrderIdForUpdate(orderId)
                        .orElseThrow(() -> new PaymentNotFoundException(
                                "The existing Razorpay order is not linked to a payment record."));

        if (payment.getBooking() == null
                || !bookingId.equals(payment.getBooking().getId())) {
            throw new PaymentConflictException(
                    "The existing Razorpay order belongs to a different booking.");
        }

        BigDecimal expectedAmount = payment.getBooking().getAgreedPrice();

        if (expectedAmount == null
                || expectedAmount.signum() <= 0
                || payment.getAmount() == null
                || payment.getAmount().compareTo(expectedAmount) != 0) {
            throw new PaymentConflictException(
                    "The local payment amount does not match the booking amount.");
        }

        long expectedPaise;

        try {
            expectedPaise = expectedAmount
                    .multiply(BigDecimal.valueOf(100))
                    .longValueExact();
        } catch (ArithmeticException ex) {
            throw new PaymentConflictException(
                    "The booking amount is invalid for Razorpay reconciliation.");
        }

        try {
            Order gatewayOrder = razorpayClient.orders.fetch(orderId);

            String returnedOrderId = value(gatewayOrder.get("id"));
            long returnedOrderAmount = parseAmount(gatewayOrder.get("amount"));
            String returnedOrderCurrency = value(gatewayOrder.get("currency"));

            if (!orderId.equals(returnedOrderId)
                    || returnedOrderAmount != expectedPaise
                    || !"INR".equalsIgnoreCase(returnedOrderCurrency)) {
                throw new PaymentConflictException(
                        "The Razorpay order ID, amount, or currency does not match.");
            }

            List<com.razorpay.Payment> gatewayPayments =
                    razorpayClient.orders.fetchPayments(orderId);

            if (gatewayPayments == null || gatewayPayments.isEmpty()) {
                throw new PaymentConflictException(
                        "Razorpay returned no payment attempts. Local payment data was not changed.");
            }

            com.razorpay.Payment capturedPayment = null;
            boolean hasPending = false;
            boolean hasAuthorized = false;
            boolean allFailed = true;
            int failedCount = 0;
            com.razorpay.Payment singleFailedPayment = null;

            for (com.razorpay.Payment gatewayPayment : gatewayPayments) {
                String associatedOrderId = value(gatewayPayment.get("order_id"));

                if (!orderId.equals(associatedOrderId)) {
                    throw new PaymentConflictException(
                            "A gateway payment references a different order.");
                }

                String gatewayStatus = value(gatewayPayment.get("status"))
                        .toLowerCase(Locale.ROOT);

                Object capturedValue = gatewayPayment.get("captured");
                boolean isCaptured = Boolean.TRUE.equals(capturedValue)
                        || "true".equalsIgnoreCase(value(capturedValue));

                if ("captured".equals(gatewayStatus) && !isCaptured) {
                    throw new PaymentConflictException(
                            "Razorpay returned inconsistent captured-payment status.");
                }

                if (isCaptured) {
                    allFailed = false;

                    long gatewayAmount = parseAmount(gatewayPayment.get("amount"));
                    String gatewayCurrency = value(gatewayPayment.get("currency"));

                    if (!"captured".equals(gatewayStatus)
                            || gatewayAmount != expectedPaise
                            || !"INR".equalsIgnoreCase(gatewayCurrency)) {
                        throw new PaymentConflictException(
                                "The captured payment does not match the expected status, amount, or currency.");
                    }

                    if (capturedPayment != null) {
                        throw new PaymentConflictException(
                                "Multiple captured payments exist for this order. Manual investigation is required.");
                    }

                    capturedPayment = gatewayPayment;
                    continue;
                }

                switch (gatewayStatus) {
                    case "created", "pending" -> {
                        hasPending = true;
                        allFailed = false;
                    }
                    case "authorized" -> {
                        hasAuthorized = true;
                        allFailed = false;
                    }
                    case "failed" -> {
                        failedCount++;
                        singleFailedPayment = gatewayPayment;
                    }
                    default -> throw new PaymentConflictException(
                            "Razorpay returned an unsupported payment status: "
                                    + gatewayStatus
                                    + ". Local payment data was not changed.");
                }
            }

            if (capturedPayment != null) {
                String gatewayPaymentId = value(capturedPayment.get("id"));

                if (gatewayPaymentId.isBlank()
                        || "null".equalsIgnoreCase(gatewayPaymentId)) {
                    throw new PaymentConflictException(
                            "Razorpay did not return a valid captured payment ID.");
                }

                if (payment.getStatus() == PaymentStatus.SUCCESS
                        && !gatewayPaymentId.equals(payment.getTransactionId())) {
                    throw new PaymentConflictException(
                            "A different transaction is already recorded as successful.");
                }

                var existingTransaction =
                        paymentRepository.findByTransactionId(gatewayPaymentId);

                if (existingTransaction.isPresent()
                        && !existingTransaction.get().getId().equals(payment.getId())) {
                    throw new PaymentConflictException(
                            "This Razorpay payment ID is already assigned to another record.");
                }

                PaymentAttempt attempt = getOrCreateAttempt(
                        bookingId, payment, orderId, expectedAmount);

                validateAttempt(attempt, bookingId, expectedAmount, gatewayPaymentId);

                attempt.setGatewayOrderId(orderId);
                attempt.setGatewayPaymentId(gatewayPaymentId);
                attempt.setAmount(expectedAmount);
                attempt.setCurrency("INR");
                attempt.setStatus(PaymentAttemptStatus.CAPTURED);

                if (attempt.getCapturedAt() == null) {
                    attempt.setCapturedAt(LocalDateTime.now());
                }

                payment.setTransactionId(gatewayPaymentId);
                payment.setStatus(PaymentStatus.SUCCESS);

                paymentAttemptRepository.saveAndFlush(attempt);

                return paymentMapper.toResponse(
                        paymentRepository.saveAndFlush(payment));
            }

            if (hasPending || hasAuthorized) {
                PaymentAttemptStatus nextAttemptStatus =
                        hasPending
                                ? PaymentAttemptStatus.PENDING
                                : PaymentAttemptStatus.AUTHORIZED;

                PaymentAttempt attempt = getOrCreateAttempt(
                        bookingId, payment, orderId, expectedAmount);

                validateAttemptWithoutNewPaymentId(
                        attempt, bookingId, expectedAmount);

                if (attempt.getStatus() == PaymentAttemptStatus.CAPTURED
                        || payment.getStatus() == PaymentStatus.SUCCESS) {
                    throw new PaymentConflictException(
                            "A previously successful payment conflicts with the current gateway response.");
                }

                attempt.setGatewayOrderId(orderId);
                attempt.setAmount(expectedAmount);
                attempt.setCurrency("INR");
                attempt.setStatus(nextAttemptStatus);

                payment.setStatus(PaymentStatus.PENDING);

                paymentAttemptRepository.saveAndFlush(attempt);

                return paymentMapper.toResponse(
                        paymentRepository.saveAndFlush(payment));
            }

            if (allFailed && failedCount > 0) {
                if (payment.getStatus() == PaymentStatus.SUCCESS) {
                    throw new PaymentConflictException(
                            "The local payment is already successful, but Razorpay returned no captured payment.");
                }

                PaymentAttempt attempt = getOrCreateAttempt(
                        bookingId, payment, orderId, expectedAmount);

                validateAttemptWithoutNewPaymentId(
                        attempt, bookingId, expectedAmount);

                if (attempt.getStatus() == PaymentAttemptStatus.CAPTURED) {
                    throw new PaymentConflictException(
                            "The attempt is already marked captured. Manual investigation is required.");
                }

                if (failedCount == 1 && singleFailedPayment != null) {
                    String failedPaymentId = value(singleFailedPayment.get("id"));

                    if (failedPaymentId.isBlank()
                            || "null".equalsIgnoreCase(failedPaymentId)) {
                        throw new PaymentConflictException(
                                "Razorpay did not return a valid failed payment ID.");
                    }

                    var existingTransaction =
                            paymentRepository.findByTransactionId(failedPaymentId);

                    if (existingTransaction.isPresent()
                            && !existingTransaction.get().getId().equals(payment.getId())) {
                        throw new PaymentConflictException(
                                "The failed payment ID is already assigned to another payment record.");
                    }

                    if (attempt.getGatewayPaymentId() != null
                            && !attempt.getGatewayPaymentId().equals(failedPaymentId)
                            && attempt.getStatus() != PaymentAttemptStatus.FAILED
                            && attempt.getStatus() != PaymentAttemptStatus.CANCELLED) {
                        throw new PaymentConflictException(
                                "The attempt references a different gateway payment.");
                    }

                    attempt.setGatewayPaymentId(failedPaymentId);
                } else if (attempt.getGatewayPaymentId() != null
                        && attempt.getStatus() != PaymentAttemptStatus.FAILED
                        && attempt.getStatus() != PaymentAttemptStatus.CANCELLED) {
                    throw new PaymentConflictException(
                            "Multiple failed gateway attempts conflict with the existing attempt record.");
                }

                attempt.setGatewayOrderId(orderId);
                attempt.setAmount(expectedAmount);
                attempt.setCurrency("INR");
                attempt.setStatus(PaymentAttemptStatus.FAILED);

                payment.setStatus(PaymentStatus.FAILED);

                paymentAttemptRepository.saveAndFlush(attempt);

                return paymentMapper.toResponse(
                        paymentRepository.saveAndFlush(payment));
            }

            throw new PaymentConflictException(
                    "Razorpay returned an ambiguous payment result. Local payment data was not changed.");

        } catch (PaymentConflictException | PaymentNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error(
                    "Razorpay reconciliation failed for booking {} and order {}",
                    bookingId,
                    orderId,
                    ex);

            throw new PaymentConflictException(
                    "Unable to verify the existing Razorpay order. "
                            + "No new order was created. Check the gateway credentials "
                            + "and server logs before retrying.");
        }
    }

    private PaymentAttempt getOrCreateAttempt(
            Long bookingId,
            com.application.Servease.payment.entity.Payment payment,
            String orderId,
            BigDecimal expectedAmount) {

        PaymentAttempt attempt = paymentAttemptRepository
                .findByGatewayOrderId(orderId)
                .orElse(null);

        if (attempt != null) {
            return attempt;
        }

        List<PaymentAttempt> attempts = paymentAttemptRepository
                .findByBookingIdOrderByAttemptNumberDesc(bookingId);

        boolean conflictingCapturedAttempt = attempts.stream()
                .anyMatch(existing ->
                        existing.getStatus() == PaymentAttemptStatus.CAPTURED
                                && existing.getGatewayPaymentId() != null);

        if (conflictingCapturedAttempt) {
            throw new PaymentConflictException(
                    "Another captured payment exists for this booking. Manual investigation is required.");
        }

        int nextAttemptNumber = attempts.stream()
                .map(PaymentAttempt::getAttemptNumber)
                .filter(number -> number != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        return PaymentAttempt.builder()
                .booking(payment.getBooking())
                .attemptNumber(nextAttemptNumber)
                .provider("RAZORPAY")
                .gatewayOrderId(orderId)
                .amount(expectedAmount)
                .currency("INR")
                .status(PaymentAttemptStatus.PENDING)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
    }

    private void validateAttempt(
            PaymentAttempt attempt,
            Long bookingId,
            BigDecimal expectedAmount,
            String gatewayPaymentId) {

        validateAttemptWithoutNewPaymentId(
                attempt, bookingId, expectedAmount);

        if (attempt.getStatus() == PaymentAttemptStatus.CAPTURED
                && attempt.getGatewayPaymentId() != null
                && !gatewayPaymentId.equals(attempt.getGatewayPaymentId())) {
            throw new PaymentConflictException(
                    "A different captured payment is already recorded for this attempt.");
        }

        if (attempt.getGatewayPaymentId() != null
                && !gatewayPaymentId.equals(attempt.getGatewayPaymentId())
                && attempt.getStatus() != PaymentAttemptStatus.FAILED
                && attempt.getStatus() != PaymentAttemptStatus.CANCELLED) {
            throw new PaymentConflictException(
                    "The existing attempt references a different gateway payment.");
        }
    }

    private void validateAttemptWithoutNewPaymentId(
            PaymentAttempt attempt,
            Long bookingId,
            BigDecimal expectedAmount) {

        if (attempt.getBooking() == null
                || !bookingId.equals(attempt.getBooking().getId())) {
            throw new PaymentConflictException(
                    "The existing payment attempt belongs to another booking.");
        }

        if (attempt.getAmount() == null
                || attempt.getAmount().compareTo(expectedAmount) != 0
                || !"INR".equalsIgnoreCase(attempt.getCurrency())) {
            throw new PaymentConflictException(
                    "The payment attempt amount or currency conflicts with the booking.");
        }
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private long parseAmount(Object value) {
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new PaymentConflictException(
                    "Razorpay returned an invalid payment amount.");
        }
    }
}
