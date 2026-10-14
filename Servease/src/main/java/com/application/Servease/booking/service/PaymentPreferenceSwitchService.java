
package com.application.Servease.booking.service;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.booking.enums.PaymentPreference;
import com.application.Servease.booking.repository.BookingRepository;
import com.application.Servease.common.exception.BookingNotFoundException;
import com.application.Servease.common.exception.InvalidBookingStatusException;
import com.application.Servease.common.exception.PaymentConflictException;
import com.application.Servease.payment.entity.Payment;
import com.application.Servease.payment.enums.PaymentStatus;
import com.application.Servease.payment.repository.PaymentRepository;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentPreferenceSwitchService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final UserService userService;

    @Transactional
    public void switchToPayAtService(Long bookingId) {
        User customer = userService.getAuthenticatedUser();

        Booking booking = bookingRepository
                .findByIdAndCustomerForUpdate(bookingId, customer)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found."
                ));

        if (booking.getPaymentPreference() != PaymentPreference.PAY_ONLINE) {
            throw new PaymentConflictException(
                    "Only bookings originally configured for online payment can use this switch."
            );
        }

        if ("PAY_AT_SERVICE".equals(
                booking.getEffectivePaymentPreference())) {
            return;
        }

        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new InvalidBookingStatusException(
                    "Payment preference can only be switched for an accepted booking."
            );
        }

        LocalDateTime appointmentStart = LocalDateTime.of(
                booking.getBookingDate(),
                booking.getBookingTime()
        );

        if (!LocalDateTime.now().isBefore(appointmentStart)) {
            throw new InvalidBookingStatusException(
                    "Payment preference cannot be switched once the appointment has started."
            );
        }

        Payment payment = paymentRepository
                .findByBookingId(bookingId)
                .orElse(null);

        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new PaymentConflictException(
                    "A successful online payment cannot be switched to pay-at-service."
            );
        }

        if (payment != null
                && payment.getGatewayOrderId() != null
                && !payment.getGatewayOrderId().isBlank()) {
            throw new PaymentConflictException(
                    "A Razorpay order already exists for this booking. Its status must be reconciled before switching payment preference."
            );
        }

        booking.setEffectivePaymentPreference(
                PaymentPreference.PAY_AT_SERVICE.name()
        );
        booking.setPaymentPreferenceSwitchedAt(LocalDateTime.now());

        bookingRepository.save(booking);
    }
}
