package com.application.Servease.payment.service;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.booking.enums.PaymentPreference;
import com.application.Servease.booking.repository.BookingRepository;
import com.application.Servease.common.exception.BookingNotFoundException;
import com.application.Servease.common.exception.PaymentConflictException;
import com.application.Servease.common.exception.PaymentNotFoundException;
import com.application.Servease.payment.dto.CreatePaymentRequest;
import com.application.Servease.payment.dto.PaymentResponse;
import com.application.Servease.payment.entity.Payment;
import com.application.Servease.payment.entity.PaymentAttempt;
import com.application.Servease.payment.enums.PaymentAttemptStatus;
import com.application.Servease.payment.enums.PaymentMethod;
import com.application.Servease.payment.enums.PaymentStatus;
import com.application.Servease.payment.mapper.PaymentMapper;
import com.application.Servease.payment.repository.PaymentAttemptRepository;
import com.application.Servease.payment.repository.PaymentRepository;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final BookingRepository bookingRepository;
    private final PaymentMapper paymentMapper;
    private final UserService userService;

    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        if (request == null || request.getBookingId() == null) {
            throw new PaymentConflictException(
                    "A valid booking ID is required."
            );
        }

        if (request.getPaymentMethod() == null) {
            throw new PaymentConflictException(
                    "A payment method is required."
            );
        }

        User customer = userService.getAuthenticatedUser();

        Booking booking = bookingRepository
                .findByIdAndCustomerForUpdate(request.getBookingId(), customer)
                .orElseThrow(() ->
                        new BookingNotFoundException("Booking not found."));

        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new PaymentConflictException(
                    "Payment can only be created for an accepted booking."
            );
        }

        PaymentPreference effectivePreference =
                getEffectivePreference(booking);

        if (effectivePreference != PaymentPreference.PAY_ONLINE) {
            throw new PaymentConflictException(
                    "This booking is configured for payment at service."
            );
        }

        if (request.getPaymentMethod() == PaymentMethod.CASH) {
            throw new PaymentConflictException(
                    "Cash is not allowed for an online payment booking."
            );
        }

        if (booking.getPaymentDeadlineAt() != null
                && !LocalDateTime.now().isBefore(
                booking.getPaymentDeadlineAt())) {
            throw new PaymentConflictException(
                    "The online payment deadline has expired."
            );
        }

        BigDecimal amount = booking.getAgreedPrice();

        if (amount == null || amount.signum() <= 0) {
            throw new PaymentConflictException(
                    "The booking amount is invalid."
            );
        }

        List<PaymentAttempt> attempts = paymentAttemptRepository
                .findByBookingIdOrderByAttemptNumberDesc(booking.getId());

        boolean hasCapturedAttempt = attempts.stream()
                .anyMatch(attempt ->
                        attempt.getStatus() == PaymentAttemptStatus.CAPTURED);

        if (hasCapturedAttempt) {
            throw new PaymentConflictException(
                    "A captured payment attempt already exists for this booking."
            );
        }

        Payment existingPayment = paymentRepository
                .findByBookingId(booking.getId())
                .orElse(null);

        if (existingPayment != null) {
            if (existingPayment.getStatus() == PaymentStatus.SUCCESS) {
                throw new PaymentConflictException(
                        "Payment already succeeded for this booking."
                );
            }

            return paymentMapper.toResponse(existingPayment);
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(amount)
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        int nextAttemptNumber = attempts.stream()
                .map(PaymentAttempt::getAttemptNumber)
                .filter(number -> number != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        PaymentAttempt attempt = PaymentAttempt.builder()
                .booking(booking)
                .attemptNumber(nextAttemptNumber)
                .provider("RAZORPAY")
                .amount(amount)
                .currency("INR")
                .status(PaymentAttemptStatus.CREATED)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        paymentAttemptRepository.saveAndFlush(attempt);

        return paymentMapper.toResponse(savedPayment);
    }

    private PaymentPreference getEffectivePreference(Booking booking) {
        String effective = booking.getEffectivePaymentPreference();

        if (effective != null && !effective.isBlank()) {
            try {
                return PaymentPreference.valueOf(effective);
            } catch (IllegalArgumentException ex) {
                throw new PaymentConflictException(
                        "The booking has an invalid effective payment preference. "
                                + "Please contact support."
                );
            }
        }

        PaymentPreference original = booking.getPaymentPreference();

        if (original == null) {
            throw new PaymentConflictException(
                    "The booking has no recorded payment preference. "
                            + "Please contact support."
            );
        }

        return original;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBooking(Long bookingId) {
        User customer = userService.getAuthenticatedUser();

        Booking booking = bookingRepository
                .findByIdAndCustomer(bookingId, customer)
                .orElseThrow(() ->
                        new BookingNotFoundException("Booking not found."));

        Payment payment = paymentRepository
                .findByBookingId(booking.getId())
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found for this booking."));

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments() {
        User customer = userService.getAuthenticatedUser();

        return bookingRepository
                .findByCustomerOrderByCreatedAtDesc(customer)
                .stream()
                .map(Booking::getId)
                .map(paymentRepository::findByBookingId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(paymentMapper::toResponse)
                .toList();
    }
}
