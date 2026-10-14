
package com.application.Servease.payment.service;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.booking.enums.PaymentPreference;
import com.application.Servease.booking.repository.BookingRepository;
import com.application.Servease.common.exception.BookingNotFoundException;
import com.application.Servease.common.exception.InvalidBookingStatusException;
import com.application.Servease.common.exception.ProviderNotFoundException;
import com.application.Servease.payment.dto.ConfirmBookingCollectionRequest;
import com.application.Servease.payment.entity.BookingCollection;
import com.application.Servease.payment.entity.Payment;
import com.application.Servease.payment.enums.CollectionMethod;
import com.application.Servease.payment.enums.CollectionStatus;
import com.application.Servease.payment.enums.PaymentStatus;
import com.application.Servease.payment.repository.BookingCollectionRepository;
import com.application.Servease.payment.repository.PaymentRepository;
import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingCollectionService {

    private final BookingRepository bookingRepository;
    private final BookingCollectionRepository collectionRepository;
    private final PaymentRepository paymentRepository;
    private final UserService userService;

    @Transactional
    public void confirmCollection(
            Long bookingId,
            ConfirmBookingCollectionRequest request
    ) {
        if (bookingId == null || request == null
                || request.getCollectionMethod() == null) {
            throw new InvalidBookingStatusException(
                    "A valid booking ID and collection method are required."
            );
        }

        if (request.getCollectionMethod() == CollectionMethod.ONLINE) {
            throw new InvalidBookingStatusException(
                    "Online collection confirmation is temporarily unavailable "
                            + "until captured Razorpay payment attempts are integrated."
            );
        }

        if (request.getCollectionMethod() != CollectionMethod.CASH) {
            throw new InvalidBookingStatusException(
                    "Select a supported collection method."
            );
        }

        User providerUser = userService.getAuthenticatedUser();
        ProviderProfile provider = providerUser.getProviderProfile();

        if (provider == null || provider.getId() == null) {
            throw new ProviderNotFoundException(
                    "Provider profile not found."
            );
        }

        Booking booking = bookingRepository
                .findByIdAndProviderServiceProviderForUpdate(
                        bookingId,
                        provider
                )
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found."
                ));

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new InvalidBookingStatusException(
                    "Collection can only be confirmed for an in-progress booking."
            );
        }

        String effectivePreference = booking.getEffectivePaymentPreference();

        if (effectivePreference == null || effectivePreference.isBlank()) {
            PaymentPreference originalPreference =
                    booking.getPaymentPreference();

            if (originalPreference == null) {
                throw new InvalidBookingStatusException(
                        "Payment preference is missing."
                );
            }

            effectivePreference = originalPreference.name();
        }

        if (!PaymentPreference.PAY_AT_SERVICE.name().equals(effectivePreference)) {
            throw new InvalidBookingStatusException(
                    "This booking is not configured for payment at service."
            );
        }

        BigDecimal bookingAmount = booking.getAgreedPrice();

        if (bookingAmount == null || bookingAmount.signum() <= 0) {
            throw new InvalidBookingStatusException(
                    "The saved booking amount is invalid."
            );
        }

        Payment payment = paymentRepository
                .findByBookingId(bookingId)
                .orElse(null);

        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new InvalidBookingStatusException(
                    "A successful payment already exists. Do not collect payment again."
            );
        }

        BookingCollection existing = collectionRepository
                .findByBookingId(bookingId)
                .orElse(null);

        if (existing != null
                && existing.getStatus() == CollectionStatus.CONFIRMED) {
            throw new InvalidBookingStatusException(
                    "Payment collection has already been confirmed."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        BookingCollection collection;

        if (existing == null) {
            collection = BookingCollection.builder()
                    .booking(booking)
                    .amount(bookingAmount)
                    .currency("INR")
                    .build();
        } else {
            collection = existing;
            collection.setAmount(bookingAmount);
            collection.setCurrency("INR");
        }

        collection.setCollectionMethod(CollectionMethod.CASH);
        collection.setStatus(CollectionStatus.CONFIRMED);
        collection.setConfirmedBy(providerUser);
        collection.setConfirmedAt(now);

        collectionRepository.saveAndFlush(collection);
    }
}

