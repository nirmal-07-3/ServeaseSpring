package com.application.Servease.payment.service;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.booking.repository.BookingRepository;
import com.application.Servease.common.exception.BookingNotFoundException;
import com.application.Servease.common.exception.PaymentConflictException;
import com.application.Servease.common.exception.PaymentNotFoundException;
import com.application.Servease.payment.dto.CreatePaymentRequest;
import com.application.Servease.payment.dto.PaymentResponse;
import com.application.Servease.payment.entity.Payment;
import com.application.Servease.payment.enums.PaymentStatus;
import com.application.Servease.payment.mapper.PaymentMapper;
import com.application.Servease.payment.repository.PaymentRepository;
import com.application.Servease.payment.service.PaymentService;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    private final BookingRepository bookingRepository;

    private final PaymentMapper paymentMapper;

    private final UserService userService;

    @Override
    public PaymentResponse createPayment(
            CreatePaymentRequest request) {

        // 1. Get authenticated customer
        User customer =
                userService.getAuthenticatedUser();

        // 2. Find booking belonging to this customer
        Booking booking =
                bookingRepository
                        .findByIdAndCustomer(
                                request.getBookingId(),
                                customer
                        )
                        .orElseThrow(() ->
                                new BookingNotFoundException(
                                        "Booking not found."
                                ));

        // 3. Booking must be accepted
        if (booking.getStatus()
                != BookingStatus.ACCEPTED) {

            throw new PaymentConflictException(
                    "Payment can only be created for an accepted booking."
            );
        }

        // 4. Check whether payment already exists
        if (paymentRepository.existsByBookingId(
                booking.getId())) {

            throw new PaymentConflictException(
                    "Payment already exists for this booking."
            );
        }

        // 5. Create payment
        Payment payment = Payment.builder()
                .booking(booking)

                // Never trust amount from frontend
                .amount(booking.getAgreedPrice())

                .paymentMethod(
                        request.getPaymentMethod()
                )

                .status(PaymentStatus.PENDING)

                .build();

        Payment savedPayment =
                paymentRepository.save(payment);

        return paymentMapper.toResponse(
                savedPayment
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBooking(
            Long bookingId) {

        User customer =
                userService.getAuthenticatedUser();

        // Make sure booking belongs to customer
        Booking booking =
                bookingRepository
                        .findByIdAndCustomer(
                                bookingId,
                                customer
                        )
                        .orElseThrow(() ->
                                new BookingNotFoundException(
                                        "Booking not found."
                                ));

        Payment payment =
                paymentRepository
                        .findByBookingId(
                                booking.getId()
                        )
                        .orElseThrow(() ->
                                new PaymentNotFoundException(
                                        "Payment not found for this booking."
                                ));

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments() {

        User customer =
                userService.getAuthenticatedUser();

        return bookingRepository
                .findByCustomerOrderByCreatedAtDesc(
                        customer
                )
                .stream()
                .map(Booking::getId)
                .map(paymentRepository::findByBookingId)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(paymentMapper::toResponse)
                .toList();
    }
}