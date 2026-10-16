
package com.application.Servease.payment.service;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.booking.enums.PaymentPreference;
import com.application.Servease.booking.repository.BookingRepository;
import com.application.Servease.common.exception.BookingNotFoundException;
import com.application.Servease.common.exception.InvalidBookingStatusException;
import com.application.Servease.common.exception.PaymentConflictException;
import com.application.Servease.common.exception.ProviderNotFoundException;
import com.application.Servease.payment.dto.BookingOnlineQrResponse;
import com.application.Servease.payment.dto.DevelopmentCollectionSimulationRequest;
import com.application.Servease.payment.entity.BookingCollection;
import com.application.Servease.payment.entity.Payment;
import com.application.Servease.payment.entity.PaymentAttempt;
import com.application.Servease.payment.enums.CollectionMethod;
import com.application.Servease.payment.enums.CollectionStatus;
import com.application.Servease.payment.enums.PaymentAttemptStatus;
import com.application.Servease.payment.enums.PaymentStatus;
import com.application.Servease.payment.repository.BookingCollectionRepository;
import com.application.Servease.payment.repository.PaymentAttemptRepository;
import com.application.Servease.payment.repository.PaymentRepository;
import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DevelopmentCollectionSimulationService {

    private static final String SIMULATOR_PROVIDER = "DEV_SIMULATOR";

    private final BookingRepository bookingRepository;
    private final BookingCollectionRepository collectionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final UserService userService;

    @Transactional
    public BookingOnlineQrResponse createPendingCollection(Long bookingId) {
        User providerUser = requireProviderUser();
        ProviderProfile provider = providerUser.getProviderProfile();

        Booking booking = bookingRepository
                .findByIdAndProviderServiceProviderForUpdate(bookingId, provider)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found."));

        validateBookingForCollection(booking);

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new InvalidBookingStatusException(
                    "A successful payment already exists. Do not collect payment again.");
        }

        BookingCollection collection = collectionRepository
                .findByBookingIdForUpdate(bookingId)
                .orElse(null);

        if (collection != null && collection.getStatus() == CollectionStatus.CONFIRMED) {
            throw new InvalidBookingStatusException(
                    "Collection has already been confirmed.");
        }

        if (collection != null
                && collection.getGatewayQrCodeId() != null
                && !collection.getGatewayQrCodeId().isBlank()) {
            throw new PaymentConflictException(
                    "A real Razorpay QR is associated with this booking. Do not replace it with a simulated collection.");
        }

        if (collection != null
                && collection.getStatus() == CollectionStatus.PENDING
                && collection.getPaymentAttempt() != null
                && !SIMULATOR_PROVIDER.equals(collection.getPaymentAttempt().getProvider())) {
            throw new PaymentConflictException(
                    "A non-simulated payment attempt is already pending for this booking.");
        }

        if (collection != null
                && collection.getStatus() == CollectionStatus.PENDING
                && collection.getPaymentAttempt() != null
                && SIMULATOR_PROVIDER.equals(collection.getPaymentAttempt().getProvider())) {
            return toResponse(collection, "TEST_PENDING");
        }

        List<PaymentAttempt> attempts = paymentAttemptRepository
                .findByBookingIdOrderByAttemptNumberDesc(bookingId);

        int nextAttemptNumber = attempts.stream()
                .map(PaymentAttempt::getAttemptNumber)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        PaymentAttempt attempt = PaymentAttempt.builder()
                .booking(booking)
                .attemptNumber(nextAttemptNumber)
                .provider(SIMULATOR_PROVIDER)
                .amount(booking.getAgreedPrice())
                .currency("INR")
                .status(PaymentAttemptStatus.PENDING)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        attempt = paymentAttemptRepository.saveAndFlush(attempt);

        if (collection == null) {
            collection = BookingCollection.builder()
                    .booking(booking)
                    .amount(booking.getAgreedPrice())
                    .currency("INR")
                    .build();
        }

        applyFinancialSnapshot(collection, booking);
        collection.setAmount(booking.getAgreedPrice());
        collection.setCurrency("INR");
        collection.setCollectionMethod(CollectionMethod.ONLINE);
        collection.setStatus(CollectionStatus.PENDING);
        collection.setPaymentAttempt(attempt);
        collection.setGatewayQrCodeId(null);
        collection.setGatewayQrImageUrl(createTestImage());
        collection.setQrCloseByEpoch(null);
        collection.setGatewayPaymentId(null);
        collection.setConfirmedAt(null);
        collection.setConfirmedBy(null);

        collection = collectionRepository.saveAndFlush(collection);

        return toResponse(collection, "TEST_PENDING");
    }

    @Transactional
    public BookingOnlineQrResponse completeSimulation(
            Long bookingId,
            DevelopmentCollectionSimulationRequest request) {

        if (request == null || request.getOutcome() == null
                || request.getOutcome().isBlank()) {
            throw new InvalidBookingStatusException(
                    "Choose SUCCESS or FAILED for the simulated outcome.");
        }

        String outcome = request.getOutcome().trim().toUpperCase(Locale.ROOT);
        if (!outcome.equals("SUCCESS") && !outcome.equals("FAILED")) {
            throw new InvalidBookingStatusException(
                    "The simulated outcome must be SUCCESS or FAILED.");
        }

        User providerUser = requireProviderUser();
        ProviderProfile provider = providerUser.getProviderProfile();

        Booking booking = bookingRepository
                .findByIdAndProviderServiceProviderForUpdate(bookingId, provider)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found."));

        validateBookingForCollection(booking);

        BookingCollection collection = collectionRepository
                .findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new InvalidBookingStatusException(
                        "Create a development test collection before simulating its result."));

        PaymentAttempt attempt = collection.getPaymentAttempt();

        if (attempt == null
                || !SIMULATOR_PROVIDER.equals(attempt.getProvider())
                || !attempt.getBooking().getId().equals(bookingId)) {
            throw new PaymentConflictException(
                    "This booking does not have an active development simulator attempt.");
        }

        if (collection.getGatewayQrCodeId() != null
                && !collection.getGatewayQrCodeId().isBlank()) {
            throw new PaymentConflictException(
                    "A real Razorpay QR is associated with this collection. Simulation is blocked.");
        }

        if (collection.getStatus() == CollectionStatus.CONFIRMED) {
            if (outcome.equals("SUCCESS")
                    && attempt.getStatus() == PaymentAttemptStatus.CAPTURED) {
                return toResponse(collection, "TEST_SUCCESS");
            }
            throw new InvalidBookingStatusException(
                    "The test collection has already been confirmed.");
        }

        if (collection.getStatus() != CollectionStatus.PENDING
                || attempt.getStatus() != PaymentAttemptStatus.PENDING) {
            throw new InvalidBookingStatusException(
                    "Only a pending development test collection can be completed.");
        }

        if (outcome.equals("SUCCESS")) {
            String testTransactionId = "TEST-" + UUID.randomUUID();

            attempt.setStatus(PaymentAttemptStatus.CAPTURED);
            attempt.setGatewayPaymentId(testTransactionId);
            attempt.setCapturedAt(LocalDateTime.now());

            collection.setStatus(CollectionStatus.CONFIRMED);
            collection.setGatewayPaymentId(testTransactionId);
            collection.setConfirmedAt(LocalDateTime.now());
            collection.setConfirmedBy(providerUser);

            paymentAttemptRepository.saveAndFlush(attempt);
            collection = collectionRepository.saveAndFlush(collection);

            return toResponse(collection, "TEST_SUCCESS");
        }

        attempt.setStatus(PaymentAttemptStatus.FAILED);
        attempt.setGatewayPaymentId(null);

        collection.setStatus(CollectionStatus.FAILED);
        collection.setGatewayPaymentId(null);
        collection.setConfirmedAt(null);
        collection.setConfirmedBy(null);

        paymentAttemptRepository.saveAndFlush(attempt);
        collection = collectionRepository.saveAndFlush(collection);

        return toResponse(collection, "TEST_FAILED");
    }

    private User requireProviderUser() {
        User user = userService.getAuthenticatedUser();
        ProviderProfile provider = user.getProviderProfile();

        if (provider == null || provider.getId() == null) {
            throw new ProviderNotFoundException("Provider profile not found.");
        }

        return user;
    }

    private void validateBookingForCollection(Booking booking) {
        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new InvalidBookingStatusException(
                    "A collection can only be tested for an in-progress booking.");
        }

        if (resolvePreference(booking) != PaymentPreference.PAY_AT_SERVICE) {
            throw new InvalidBookingStatusException(
                    "This booking is not configured for Pay at Service.");
        }

        if (booking.getAgreedPrice() == null
                || booking.getAgreedPrice().signum() <= 0) {
            throw new InvalidBookingStatusException(
                    "The saved booking amount is invalid.");
        }
    }

    private PaymentPreference resolvePreference(Booking booking) {
        String effective = booking.getEffectivePaymentPreference();

        if (effective != null && !effective.isBlank()) {
            try {
                return PaymentPreference.valueOf(effective);
            } catch (IllegalArgumentException ex) {
                throw new InvalidBookingStatusException(
                        "The booking has an invalid effective payment preference.");
            }
        }

        if (booking.getPaymentPreference() == null) {
            throw new InvalidBookingStatusException(
                    "Payment preference is missing.");
        }

        return booking.getPaymentPreference();
    }

    private void applyFinancialSnapshot(
            BookingCollection collection,
            Booking booking) {

        BigDecimal amount = booking.getAgreedPrice();

        BigDecimal feeRate = booking.getPlatformFeeRateSnapshot() == null
                ? BigDecimal.ZERO
                : booking.getPlatformFeeRateSnapshot();

        BigDecimal feeAmount = booking.getPlatformFeeAmountSnapshot();

        if (feeAmount == null) {
            feeAmount = amount.multiply(feeRate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }

        feeAmount = feeAmount.setScale(2, RoundingMode.HALF_UP);

        if (feeRate.signum() < 0
                || feeRate.compareTo(new BigDecimal("100")) > 0
                || feeAmount.signum() < 0
                || feeAmount.compareTo(amount) > 0) {
            throw new InvalidBookingStatusException(
                    "The saved platform fee snapshot is invalid.");
        }

        collection.setPlatformFeeRateSnapshot(
                feeRate.setScale(2, RoundingMode.HALF_UP));
        collection.setPlatformFeeAmountSnapshot(feeAmount);
        collection.setProviderNetAmountSnapshot(
                amount.subtract(feeAmount).setScale(2, RoundingMode.HALF_UP));
    }

    private String createTestImage() {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="320" height="320" viewBox="0 0 320 320">
                  <rect width="320" height="320" rx="18" fill="#101827"/>
                  <rect x="18" y="18" width="284" height="284" rx="12" fill="#18263b" stroke="#5b8def" stroke-width="2"/>
                  <text x="160" y="105" fill="#8fb5ff" font-family="Arial,sans-serif" font-size="24" font-weight="700" text-anchor="middle">SERVEASE</text>
                  <text x="160" y="150" fill="#ffffff" font-family="Arial,sans-serif" font-size="20" font-weight="700" text-anchor="middle">TEST ONLY</text>
                  <text x="160" y="184" fill="#ffd479" font-family="Arial,sans-serif" font-size="15" text-anchor="middle">NOT A REAL QR CODE</text>
                  <text x="160" y="225" fill="#c2cde0" font-family="Arial,sans-serif" font-size="13" text-anchor="middle">No money will be collected</text>
                </svg>
                """;

        return "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(
                svg.getBytes(StandardCharsets.UTF_8));
    }

    private BookingOnlineQrResponse toResponse(
            BookingCollection collection,
            String status) {

        return BookingOnlineQrResponse.builder()
                .bookingId(collection.getBooking().getId())
                .collectionId(collection.getId())
                .amount(collection.getAmount())
                .currency(collection.getCurrency())
                .qrCodeId(null)
                .gatewayPaymentId(collection.getGatewayPaymentId())
                .imageUrl(collection.getGatewayQrImageUrl())
                .status(status)
                .createdAt(collection.getCreatedAt())
                .expiresAt(null)
                .build();
    }
}
