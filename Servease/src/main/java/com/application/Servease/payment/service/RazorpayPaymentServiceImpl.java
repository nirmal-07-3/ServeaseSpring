
package com.application.Servease.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.booking.enums.PaymentPreference;
import com.application.Servease.booking.repository.BookingRepository;
import com.application.Servease.common.exception.BookingNotFoundException;
import com.application.Servease.common.exception.PaymentConflictException;
import com.application.Servease.common.exception.PaymentNotFoundException;
import com.application.Servease.payment.dto.PaymentResponse;
import com.application.Servease.payment.dto.RazorpayOrderResponse;
import com.application.Servease.payment.dto.VerifyPaymentRequest;
import com.application.Servease.payment.entity.Payment;
import com.application.Servease.payment.entity.PaymentAttempt;
import com.application.Servease.payment.enums.PaymentAttemptStatus;
import com.application.Servease.payment.enums.PaymentStatus;
import com.application.Servease.payment.mapper.PaymentMapper;
import com.application.Servease.payment.repository.PaymentAttemptRepository;
import com.application.Servease.payment.repository.PaymentRepository;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.service.UserService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RazorpayPaymentServiceImpl implements RazorpayPaymentService {

    private final RazorpayClient razorpayClient;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final UserService userService;
    private final PaymentMapper paymentMapper;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private static final Logger log =
            LoggerFactory.getLogger(RazorpayPaymentServiceImpl.class);

    @Override
    public RazorpayOrderResponse createOrder(Long bookingId) {
        User customer = userService.getAuthenticatedUser();

        Booking booking = bookingRepository
                .findByIdAndCustomerForUpdate(bookingId, customer)
                .orElseThrow(() ->
                        new BookingNotFoundException("Booking not found."));

        validateOnlinePaymentEligibility(booking, true);

        Payment payment = paymentRepository
                .findByBookingId(bookingId)
                .orElseThrow(() ->
                        new PaymentConflictException(
                                "Payment record not found. Create the payment first."));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new PaymentConflictException(
                    "This booking has already been paid.");
        }

        PaymentAttempt attempt = findOrCreateAttempt(booking, payment);

        if (payment.getGatewayOrderId() != null
                && !payment.getGatewayOrderId().isBlank()) {
            try {
                Order existingOrder =
                        razorpayClient.orders.fetch(payment.getGatewayOrderId());

                long expectedPaise = toPaise(booking.getAgreedPrice());
                long actualPaise = Long.parseLong(
                        String.valueOf((Object) existingOrder.get("amount")));

                String currency = String.valueOf((Object) existingOrder.get("currency"));

                if (actualPaise != expectedPaise
                        || !"INR".equalsIgnoreCase(currency)) {
                    throw new PaymentConflictException(
                            "The existing Razorpay order does not match the booking amount.");
                }

                if (attempt.getGatewayOrderId() != null
                        && !attempt.getGatewayOrderId().equals(
                        payment.getGatewayOrderId())) {
                    throw new PaymentConflictException(
                            "The payment attempt references a different gateway order.");
                }

                attempt.setGatewayOrderId(payment.getGatewayOrderId());

                if (attempt.getStatus() == PaymentAttemptStatus.CREATED) {
                    attempt.setStatus(PaymentAttemptStatus.PENDING);
                }

                paymentAttemptRepository.saveAndFlush(attempt);

                return RazorpayOrderResponse.builder()
                        .paymentId(payment.getId())
                        .bookingId(booking.getId())
                        .razorpayOrderId(payment.getGatewayOrderId())
                        .razorpayKeyId(razorpayKeyId)
                        .amount(booking.getAgreedPrice())
                        .currency("INR")
                        .status(String.valueOf((Object) existingOrder.get("status")))
                        .build();
            } catch (PaymentConflictException ex) {
                throw ex;
            } catch (Exception ex) {
                log.error(
                        "Failed to verify existing Razorpay order. bookingId={}",
                        bookingId,
                        ex
                );
                throw new PaymentConflictException(
                        "Unable to verify the existing Razorpay order. "
                                + "Please contact support before retrying.");
            }
        }

        if (attempt.getGatewayOrderId() != null
                && !attempt.getGatewayOrderId().isBlank()) {
            throw new PaymentConflictException(
                    "The payment attempt already references a gateway order "
                            + "that differs from the payment record.");
        }

        long amountInPaise = toPaise(booking.getAgreedPrice());

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "SERVEASE_" + booking.getId());

        try {
            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String orderId = razorpayOrder.get("id");

            if (orderId == null || orderId.isBlank()) {
                throw new PaymentConflictException(
                        "The payment gateway did not return an order ID.");
            }

            payment.setGatewayOrderId(orderId);
            payment.setStatus(PaymentStatus.PENDING);

            attempt.setGatewayOrderId(orderId);
            attempt.setStatus(PaymentAttemptStatus.PENDING);

            paymentRepository.saveAndFlush(payment);
            paymentAttemptRepository.saveAndFlush(attempt);

            return RazorpayOrderResponse.builder()
                    .paymentId(payment.getId())
                    .bookingId(booking.getId())
                    .razorpayOrderId(orderId)
                    .razorpayKeyId(razorpayKeyId)
                    .amount(booking.getAgreedPrice())
                    .currency("INR")
                    .status("created")
                    .build();
        } catch (PaymentConflictException ex) {
            throw ex;
        } catch (Exception ex) {
        log.error(
                "Failed to create or save Razorpay order. bookingId={}",
                bookingId,
                ex
        );
        throw new PaymentConflictException(
                "Unable to create or save the Razorpay order. "
                        + "Check the payment status before retrying.");
    }
    }

    @Override
    public PaymentResponse verifyPayment(VerifyPaymentRequest request) {
        if (request == null
                || request.getRazorpayPaymentId() == null
                || request.getRazorpayPaymentId().isBlank()
                || request.getRazorpayOrderId() == null
                || request.getRazorpayOrderId().isBlank()
                || request.getRazorpaySignature() == null
                || request.getRazorpaySignature().isBlank()) {
            throw new PaymentConflictException(
                    "Complete Razorpay payment details are required.");
        }

        Payment payment = paymentRepository
                .findByGatewayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found for this Razorpay order."));

        User customer = userService.getAuthenticatedUser();

        if (!payment.getBooking().getCustomer().getId()
                .equals(customer.getId())) {
            throw new PaymentConflictException(
                    "You are not authorized to verify this payment.");
        }

        Booking booking = bookingRepository
                .findByIdAndCustomerForUpdate(
                        payment.getBooking().getId(), customer)
                .orElseThrow(() ->
                        new BookingNotFoundException("Booking not found."));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (request.getRazorpayPaymentId()
                    .equals(payment.getTransactionId())) {
                synchronizeCapturedAttempt(
                        booking,
                        payment,
                        request.getRazorpayPaymentId(),
                        request.getRazorpayOrderId());
                return paymentMapper.toResponse(payment);
            }

            throw new PaymentConflictException(
                    "A different payment has already been recorded for this booking.");
        }

        validateOnlinePaymentEligibility(booking, false);

        try {
            Utils.verifySignature(
                    request.getRazorpayOrderId()
                            + "|" + request.getRazorpayPaymentId(),
                    request.getRazorpaySignature(),
                    razorpayKeySecret);
        } catch (Exception ex) {
            throw new PaymentConflictException(
                    "Razorpay payment signature verification failed.");
        }

        try {
            Order gatewayOrder =
                    razorpayClient.orders.fetch(request.getRazorpayOrderId());

            com.razorpay.Payment gatewayPayment =
                    razorpayClient.payments.fetch(
                            request.getRazorpayPaymentId());

            long expectedPaise = toPaise(booking.getAgreedPrice());

            Object orderAmountValue = gatewayOrder.get("amount");
            Object paymentAmountValue = gatewayPayment.get("amount");


            long orderPaise = Long.parseLong(orderAmountValue.toString());
            long paymentPaise = Long.parseLong(paymentAmountValue.toString());

            Object orderCurrencyValue = gatewayOrder.get("currency");
            Object paymentCurrencyValue = gatewayPayment.get("currency");
            Object gatewayOrderIdValue = gatewayPayment.get("order_id");
            Object gatewayPaymentStatusValue = gatewayPayment.get("status");
            Object capturedValue = gatewayPayment.get("captured");

            String orderCurrency = orderCurrencyValue.toString();
            String paymentCurrency = paymentCurrencyValue.toString();
            String gatewayOrderId = gatewayOrderIdValue.toString();
            String gatewayPaymentStatus = gatewayPaymentStatusValue.toString();

            boolean captured = Boolean.TRUE.equals(capturedValue)
                    || "true".equalsIgnoreCase(capturedValue.toString());


            if (orderPaise != expectedPaise
                    || paymentPaise != expectedPaise
                    || !"INR".equalsIgnoreCase(orderCurrency)
                    || !"INR".equalsIgnoreCase(paymentCurrency)
                    || !request.getRazorpayOrderId().equals(gatewayOrderId)
                    || !"captured".equalsIgnoreCase(gatewayPaymentStatus)
                    || !captured) {
                throw new PaymentConflictException(
                        "The gateway payment is not captured or does not match "
                                + "the expected booking amount.");
            }

            PaymentAttempt attempt = findAttemptForOrder(
                    booking, request.getRazorpayOrderId());

            if (attempt.getStatus() == PaymentAttemptStatus.CAPTURED
                    && !request.getRazorpayPaymentId().equals(
                    attempt.getGatewayPaymentId())) {
                throw new PaymentConflictException(
                        "A different gateway payment is already recorded "
                                + "for this attempt.");
            }

            payment.setTransactionId(request.getRazorpayPaymentId());
            payment.setStatus(PaymentStatus.SUCCESS);

            attempt.setGatewayOrderId(request.getRazorpayOrderId());
            attempt.setGatewayPaymentId(request.getRazorpayPaymentId());
            attempt.setAmount(booking.getAgreedPrice());
            attempt.setCurrency("INR");
            attempt.setStatus(PaymentAttemptStatus.CAPTURED);
            if (attempt.getCapturedAt() == null) {
                attempt.setCapturedAt(LocalDateTime.now());
            }

            paymentAttemptRepository.saveAndFlush(attempt);
            Payment savedPayment = paymentRepository.saveAndFlush(payment);

            return paymentMapper.toResponse(savedPayment);
        } catch (PaymentConflictException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error(
                    "Failed to verify captured Razorpay payment. bookingId={}",
                    booking.getId(),
                    ex
            );
            throw new PaymentConflictException(
                    "Unable to verify the captured Razorpay payment. "
                            + "Please contact support if money was deducted.");
        }
    }

    private PaymentAttempt findOrCreateAttempt(
            Booking booking,
            Payment payment) {

        if (payment.getGatewayOrderId() != null
                && !payment.getGatewayOrderId().isBlank()) {
            var matching = paymentAttemptRepository
                    .findByGatewayOrderId(payment.getGatewayOrderId());

            if (matching.isPresent()) {
                return matching.get();
            }
        }

        List<PaymentAttempt> attempts =
                paymentAttemptRepository
                        .findByBookingIdOrderByAttemptNumberDesc(booking.getId());

        PaymentAttempt latest = attempts.isEmpty() ? null : attempts.get(0);

        if (latest != null
                && latest.getStatus() != PaymentAttemptStatus.FAILED
                && latest.getStatus() != PaymentAttemptStatus.CANCELLED
                && latest.getStatus() != PaymentAttemptStatus.REFUNDED) {
            return latest;
        }

        int nextAttemptNumber = attempts.stream()
                .map(PaymentAttempt::getAttemptNumber)
                .filter(number -> number != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        PaymentAttempt attempt = PaymentAttempt.builder()
                .booking(booking)
                .attemptNumber(nextAttemptNumber)
                .provider("RAZORPAY")
                .amount(booking.getAgreedPrice())
                .currency("INR")
                .status(PaymentAttemptStatus.CREATED)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        return paymentAttemptRepository.saveAndFlush(attempt);
    }

    private PaymentAttempt findAttemptForOrder(
            Booking booking,
            String orderId) {

        return paymentAttemptRepository.findByGatewayOrderId(orderId)
                .orElseThrow(() ->
                        new PaymentConflictException(
                                "No payment attempt is linked to this Razorpay order."));
    }

    private void synchronizeCapturedAttempt(
            Booking booking,
            Payment payment,
            String paymentId,
            String orderId) {

        PaymentAttempt attempt = findAttemptForOrder(booking, orderId);

        if (attempt.getStatus() == PaymentAttemptStatus.CAPTURED
                && !paymentId.equals(attempt.getGatewayPaymentId())) {
            throw new PaymentConflictException(
                    "A different gateway payment is already recorded for this attempt.");
        }

        attempt.setGatewayPaymentId(paymentId);
        attempt.setAmount(booking.getAgreedPrice());
        attempt.setCurrency("INR");
        attempt.setStatus(PaymentAttemptStatus.CAPTURED);

        if (attempt.getCapturedAt() == null) {
            attempt.setCapturedAt(LocalDateTime.now());
        }

        paymentAttemptRepository.saveAndFlush(attempt);
    }

    private void validateOnlinePaymentEligibility(
            Booking booking,
            boolean enforcePaymentDeadline) {

        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new PaymentConflictException(
                    "Payment can only be processed for an accepted booking.");
        }

        PaymentPreference effectivePreference =
                resolveEffectivePreference(booking);

        if (effectivePreference != PaymentPreference.PAY_ONLINE) {
            throw new PaymentConflictException(
                    "This booking is configured for payment at service.");
        }

        if (enforcePaymentDeadline) {
            LocalDateTime paymentDeadline = booking.getPaymentDeadlineAt();

            if (paymentDeadline != null
                    && !LocalDateTime.now().isBefore(paymentDeadline)) {
                throw new PaymentConflictException(
                        "The online payment deadline has expired.");
            }
        }
    }

    private PaymentPreference resolveEffectivePreference(Booking booking) {
        String effective = booking.getEffectivePaymentPreference();

        if (effective != null && !effective.isBlank()) {
            try {
                return PaymentPreference.valueOf(effective);
            } catch (IllegalArgumentException ex) {
                throw new PaymentConflictException(
                        "The booking has an invalid effective payment preference. "
                                + "Please contact support.");
            }
        }

        PaymentPreference original = booking.getPaymentPreference();

        if (original == null) {
            throw new PaymentConflictException(
                    "The booking has no recorded payment preference. "
                            + "Please contact support.");
        }

        return original;
    }

    private long toPaise(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new PaymentConflictException(
                    "The booking amount is invalid.");
        }

        try {
            return amount
                    .multiply(BigDecimal.valueOf(100))
                    .longValueExact();
        } catch (ArithmeticException ex) {
            throw new PaymentConflictException(
                    "The booking amount cannot be processed by the payment gateway.");
        }
    }
}

