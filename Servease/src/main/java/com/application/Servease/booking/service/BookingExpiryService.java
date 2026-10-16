
package com.application.Servease.booking.service;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.booking.enums.PaymentPreference;
import com.application.Servease.booking.repository.BookingRepository;
import com.application.Servease.payment.entity.BookingCollection;
import com.application.Servease.payment.entity.Payment;
import com.application.Servease.payment.entity.PaymentAttempt;
import com.application.Servease.payment.enums.CollectionStatus;
import com.application.Servease.payment.enums.PaymentAttemptStatus;
import com.application.Servease.payment.enums.PaymentStatus;
import com.application.Servease.payment.repository.BookingCollectionRepository;
import com.application.Servease.payment.repository.PaymentAttemptRepository;
import com.application.Servease.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingExpiryService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final BookingCollectionRepository bookingCollectionRepository;

    @Value("${servease.booking.expiry-check-interval-ms:60000}")
    private long expiryCheckIntervalMs;

    @Scheduled(
            fixedDelayString =
                    "${servease.booking.expiry-check-interval-ms:60000}"
    )
    @Transactional
    public void expireUnansweredBookings() {
        if (expiryCheckIntervalMs <= 0) {
            throw new IllegalStateException(
                    "Booking expiry interval must be greater than zero.");
        }

        LocalDateTime now = LocalDateTime.now();

        bookingRepository.cancelExpiredPendingBookings(
                BookingStatus.PENDING,
                BookingStatus.CANCELLED,
                "PROVIDER_TIMEOUT",
                now
        );

        log.info("Booking expiry scheduler running at {}", now);

        processExpiredOnlinePaymentBookings(now);

        log.info("Booking expiry scheduler finished at {}", LocalDateTime.now());
    }

    private void processExpiredOnlinePaymentBookings(LocalDateTime now) {
        List<Booking> bookings =
                bookingRepository.findExpiredOnlinePaymentBookingsForUpdate(
                        BookingStatus.ACCEPTED,
                        now
                );

        for (Booking booking : bookings) {
            if (booking.getPaymentPreference() != PaymentPreference.PAY_ONLINE) {
                continue;
            }

            String effectivePreference =
                    booking.getEffectivePaymentPreference();

            if (effectivePreference != null
                    && !effectivePreference.isBlank()
                    && !PaymentPreference.PAY_ONLINE.name()
                    .equals(effectivePreference)) {
                continue;
            }



            Payment payment = paymentRepository
                    .findByBookingId(booking.getId())
                    .orElse(null);

            if (payment != null
                    && payment.getStatus() != PaymentStatus.FAILED) {
                continue;
            }

            BookingCollection collection = bookingCollectionRepository
                    .findByBookingId(booking.getId())
                    .orElse(null);

            if (collection != null
                    && collection.getStatus() != CollectionStatus.FAILED
                    && collection.getStatus() != CollectionStatus.REVERSED) {
                continue;
            }

            List<PaymentAttempt> attempts =
                    paymentAttemptRepository
                            .findByBookingIdOrderByAttemptNumberDesc(
                                    booking.getId()
                            );

            boolean unresolvedAttempt = attempts.stream().anyMatch(attempt -> {
                PaymentAttemptStatus status = attempt.getStatus();

                if (status == PaymentAttemptStatus.CAPTURED
                        || status == PaymentAttemptStatus.RECONCILIATION_REQUIRED) {
                    return true;
                }

                return attempt.getGatewayOrderId() != null
                        && !attempt.getGatewayOrderId().isBlank()
                        && (status == PaymentAttemptStatus.CREATED
                        || status == PaymentAttemptStatus.PENDING
                        || status == PaymentAttemptStatus.AUTHORIZED);
            });

            if (unresolvedAttempt) {
                continue;
            }

            booking.setEffectivePaymentPreference(
                    PaymentPreference.PAY_AT_SERVICE.name()
            );
            booking.setPaymentPreferenceSwitchedAt(now);

            bookingRepository.save(booking);

            log.info(
                    "Booking {} payment preference switched from PAY_ONLINE to PAY_AT_SERVICE at {}",
                    booking.getId(),
                    now
            );
        }
    }
}
