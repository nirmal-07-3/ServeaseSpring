
package com.application.Servease.booking.service;

import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingExpiryService {

    private final BookingRepository bookingRepository;

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
    }
}
