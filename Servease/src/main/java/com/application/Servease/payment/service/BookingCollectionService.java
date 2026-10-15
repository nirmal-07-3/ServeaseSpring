
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
import com.application.Servease.payment.dto.ConfirmBookingCollectionRequest;
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
import com.razorpay.QrCode;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingCollectionService {

    private final BookingRepository bookingRepository;
    private final BookingCollectionRepository collectionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final UserService userService;
    private final RazorpayClient razorpayClient;

    @Value("${servease.booking.collection-qr-validity-minutes:60}")
    private long qrValidityMinutes;

    @Transactional
    public BookingOnlineQrResponse createOnlineQr(Long bookingId) {
        User providerUser = userService.getAuthenticatedUser();
        ProviderProfile provider = providerUser.getProviderProfile();
        if (provider == null || provider.getId() == null) {
            throw new ProviderNotFoundException("Provider profile not found.");
        }

        Booking booking = bookingRepository
                .findByIdAndProviderServiceProviderForUpdate(bookingId, provider)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found."));

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new InvalidBookingStatusException("An online collection QR can only be created for an in-progress booking.");
        }
        if (resolvePreference(booking) != PaymentPreference.PAY_AT_SERVICE) {
            throw new InvalidBookingStatusException("This booking is not configured for Pay at Service.");
        }
        if (booking.getAgreedPrice() == null || booking.getAgreedPrice().signum() <= 0) {
            throw new InvalidBookingStatusException("The saved booking amount is invalid.");
        }
        if (qrValidityMinutes < 15) {
            throw new IllegalStateException("QR validity must be at least 15 minutes for Razorpay.");
        }

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new InvalidBookingStatusException("A successful payment already exists. Do not collect payment again.");
        }

        BookingCollection collection = collectionRepository.findByBookingIdForUpdate(bookingId).orElse(null);
        if (collection != null && collection.getStatus() == CollectionStatus.CONFIRMED) {
            throw new InvalidBookingStatusException("Collection has already been confirmed.");
        }
        if (collection != null && collection.getGatewayQrCodeId() != null && !collection.getGatewayQrCodeId().isBlank()) {
            expireAndClearUnpaidQr(collection);
            if (collection.getGatewayQrCodeId() != null && !collection.getGatewayQrCodeId().isBlank()) {
                throw new InvalidBookingStatusException("A QR already exists for this booking or has payment activity. Check its status before creating another QR.");
            }
        }

        long closeBy = Instant.now().plusSeconds(qrValidityMinutes * 60).getEpochSecond();
        JSONObject request = new JSONObject();
        request.put("type", "upi_qr");
        request.put("name", "Servease Booking " + bookingId);
        request.put("usage", "single_use");
        request.put("fixed_amount", true);
        request.put("payment_amount", toPaise(booking.getAgreedPrice()));
        request.put("description", "Servease service booking " + bookingId);
        request.put("close_by", closeBy);
        JSONObject notes = new JSONObject();
        notes.put("servease_booking_id", String.valueOf(bookingId));
        notes.put("servease_collection", "PAY_AT_SERVICE");
        request.put("notes", notes);

        try {
            QrCode qrCode = razorpayClient.qrCode.create(request);
            String qrCodeId = qrCode.get("id");
            String imageUrl = qrCode.get("image_url");
            if (qrCodeId == null || qrCodeId.isBlank() || imageUrl == null || imageUrl.isBlank()) {
                throw new PaymentConflictException("Razorpay did not return a usable QR code.");
            }

            List<PaymentAttempt> attempts = paymentAttemptRepository
                    .findByBookingIdOrderByAttemptNumberDesc(bookingId);
            int nextAttemptNumber = attempts.stream()
                    .map(PaymentAttempt::getAttemptNumber)
                    .filter(value -> value != null)
                    .max(Integer::compareTo).orElse(0) + 1;
            PaymentAttempt attempt = PaymentAttempt.builder()
                    .booking(booking)
                    .attemptNumber(nextAttemptNumber)
                    .provider("RAZORPAY_QR")
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
            collection.setCollectionMethod(CollectionMethod.ONLINE);
            collection.setStatus(CollectionStatus.PENDING);
            applyFinancialSnapshot(collection, booking);
            collection.setPaymentAttempt(attempt);
            collection.setGatewayQrCodeId(qrCodeId);
            collection.setGatewayQrImageUrl(imageUrl);
            collection.setQrCloseByEpoch(closeBy);
            collection.setGatewayPaymentId(null);
            collection.setConfirmedAt(null);
            collection.setConfirmedBy(null);
            collection = collectionRepository.saveAndFlush(collection);
            return toResponse(collection, "active");

        } catch (PaymentConflictException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error(
                    "Failed to create online QR for bookingId={}. Exception type: {}",
                    bookingId,
                    ex.getClass().getName(),
                    ex
            );

            if (ex instanceof com.razorpay.RazorpayException razorpayEx) {
                log.error("Razorpay HTTP status: {}", razorpayEx.getStatusCode());
                log.error("Razorpay error response: {}", razorpayEx.getErrorResponse());
            }

            throw new PaymentConflictException(
                    "Unable to create a Razorpay QR. Check the booking collection status before retrying."
            );
        }

    }

    @Transactional(readOnly = true)
    public BookingOnlineQrResponse getCollectionStatus(Long bookingId) {
        User providerUser = userService.getAuthenticatedUser();
        ProviderProfile provider = providerUser.getProviderProfile();
        if (provider == null || provider.getId() == null) {
            throw new ProviderNotFoundException("Provider profile not found.");
        }
        bookingRepository.findByIdAndProviderServiceProvider(bookingId, provider)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found."));
        BookingCollection collection = collectionRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new InvalidBookingStatusException("No collection exists for this booking yet."));
        return toResponse(collection, collection.getStatus().name());
    }

    @Transactional
    public void confirmCollection(Long bookingId, ConfirmBookingCollectionRequest request) {
        if (bookingId == null || request == null || request.getCollectionMethod() == null) {
            throw new InvalidBookingStatusException("A valid booking ID and collection method are required.");
        }
        if (request.getCollectionMethod() == CollectionMethod.ONLINE) {
            throw new InvalidBookingStatusException("Online collection is confirmed automatically after Razorpay verifies a captured payment. Create the booking QR and wait for payment confirmation.");
        }
        if (request.getCollectionMethod() != CollectionMethod.CASH) {
            throw new InvalidBookingStatusException("Select a supported collection method.");
        }

        User providerUser = userService.getAuthenticatedUser();
        ProviderProfile provider = providerUser.getProviderProfile();
        if (provider == null || provider.getId() == null) {
            throw new ProviderNotFoundException("Provider profile not found.");
        }

        Booking booking = bookingRepository.findByIdAndProviderServiceProviderForUpdate(bookingId, provider)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found."));
        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new InvalidBookingStatusException("Collection can only be confirmed for an in-progress booking.");
        }
        if (resolvePreference(booking) != PaymentPreference.PAY_AT_SERVICE) {
            throw new InvalidBookingStatusException("This booking is not configured for payment at service.");
        }
        BigDecimal bookingAmount = booking.getAgreedPrice();
        if (bookingAmount == null || bookingAmount.signum() <= 0) {
            throw new InvalidBookingStatusException("The saved booking amount is invalid.");
        }
        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new InvalidBookingStatusException("A successful payment already exists. Do not collect payment again.");
        }
        BookingCollection existing = collectionRepository.findByBookingIdForUpdate(bookingId).orElse(null);
        if (existing != null && existing.getStatus() == CollectionStatus.CONFIRMED) {
            throw new InvalidBookingStatusException("Payment collection has already been confirmed.");
        }
        if (existing != null && existing.getGatewayQrCodeId() != null && !existing.getGatewayQrCodeId().isBlank()) {
            expireAndClearUnpaidQr(existing);
            if (existing.getGatewayQrCodeId() != null && !existing.getGatewayQrCodeId().isBlank()) {
                throw new InvalidBookingStatusException("An online QR is still active or has payment activity. Do not confirm cash until its status is reconciled.");
            }
        }
        BookingCollection collection = existing == null
                ? BookingCollection.builder().booking(booking).amount(bookingAmount).currency("INR").build()
                : existing;
        collection.setCollectionMethod(CollectionMethod.CASH);
        collection.setStatus(CollectionStatus.CONFIRMED);
        applyFinancialSnapshot(collection, booking);
        collection.setAmount(bookingAmount);
        collection.setCurrency("INR");
        collection.setConfirmedBy(providerUser);
        collection.setConfirmedAt(LocalDateTime.now());
        collectionRepository.saveAndFlush(collection);
    }

    private void expireAndClearUnpaidQr(BookingCollection collection) {
        Long closeBy = collection.getQrCloseByEpoch();
        if (closeBy == null || Instant.now().getEpochSecond() <= closeBy) {
            return;
        }
        String qrCodeId = collection.getGatewayQrCodeId();
        try {
            QrCode qrCode = razorpayClient.qrCode.fetch(qrCodeId);
            long paymentsCount = parseGatewayLong(qrCode.get("payments_count_received"));
            long amountReceived = parseGatewayLong(qrCode.get("payments_amount_received"));
            if (paymentsCount > 0 || amountReceived > 0) {
                throw new InvalidBookingStatusException("The expired QR has payment activity. Reconcile Razorpay before retrying or accepting cash.");
            }
            String status = String.valueOf((Object) qrCode.get("status"));
            if ("active".equalsIgnoreCase(status)) {
                razorpayClient.qrCode.close(qrCodeId);
                QrCode closedQrCode = razorpayClient.qrCode.fetch(qrCodeId);
                long closedPaymentsCount = parseGatewayLong(closedQrCode.get("payments_count_received"));
                long closedAmountReceived = parseGatewayLong(closedQrCode.get("payments_amount_received"));
                if (closedPaymentsCount > 0 || closedAmountReceived > 0) {
                    throw new InvalidBookingStatusException("The QR received payment activity while closing. Reconcile Razorpay before retrying or accepting cash.");
                }
            }
            PaymentAttempt attempt = collection.getPaymentAttempt();
            if (attempt != null && attempt.getStatus() != PaymentAttemptStatus.CAPTURED) {
                attempt.setStatus(PaymentAttemptStatus.CANCELLED);
                paymentAttemptRepository.saveAndFlush(attempt);
            }
            collection.setGatewayQrCodeId(null);
            collection.setGatewayQrImageUrl(null);
            collection.setQrCloseByEpoch(null);
            collection.setPaymentAttempt(null);
            collectionRepository.saveAndFlush(collection);
        } catch (InvalidBookingStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PaymentConflictException("Unable to verify whether the expired QR received a payment. Do not create another QR or accept cash until reconciliation succeeds.");
        }
    }

    private long parseGatewayLong(Object value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new PaymentConflictException("Razorpay returned an invalid QR payment counter.");
        }
    }

    private void applyFinancialSnapshot(BookingCollection collection, Booking booking) {
        BigDecimal amount = booking.getAgreedPrice();
        BigDecimal feeRate = booking.getPlatformFeeRateSnapshot() == null
                ? BigDecimal.ZERO : booking.getPlatformFeeRateSnapshot();
        BigDecimal feeAmount = booking.getPlatformFeeAmountSnapshot();
        if (feeAmount == null) {
            feeAmount = amount.multiply(feeRate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        feeAmount = feeAmount.setScale(2, RoundingMode.HALF_UP);
        if (feeRate.signum() < 0 || feeRate.compareTo(new BigDecimal("100")) > 0
                || feeAmount.signum() < 0 || feeAmount.compareTo(amount) > 0) {
            throw new InvalidBookingStatusException("The saved platform fee snapshot is invalid.");
        }
        collection.setPlatformFeeRateSnapshot(feeRate.setScale(2, RoundingMode.HALF_UP));
        collection.setPlatformFeeAmountSnapshot(feeAmount);
        collection.setProviderNetAmountSnapshot(amount.subtract(feeAmount).setScale(2, RoundingMode.HALF_UP));
    }

    private PaymentPreference resolvePreference(Booking booking) {
        String effective = booking.getEffectivePaymentPreference();
        if (effective != null && !effective.isBlank()) {
            try {
                return PaymentPreference.valueOf(effective);
            } catch (IllegalArgumentException ex) {
                throw new InvalidBookingStatusException("The booking has an invalid effective payment preference.");
            }
        }
        if (booking.getPaymentPreference() == null) {
            throw new InvalidBookingStatusException("Payment preference is missing.");
        }
        return booking.getPaymentPreference();
    }

    private long toPaise(BigDecimal amount) {
        try {
            return amount.multiply(BigDecimal.valueOf(100)).longValueExact();
        } catch (ArithmeticException ex) {
            throw new InvalidBookingStatusException("The booking amount cannot be represented in paise.");
        }
    }

    private BookingOnlineQrResponse toResponse(BookingCollection collection, String status) {
        LocalDateTime expiry = collection.getQrCloseByEpoch() == null
                ? null : LocalDateTime.ofInstant(Instant.ofEpochSecond(collection.getQrCloseByEpoch()), ZoneId.systemDefault());
        return BookingOnlineQrResponse.builder()
                .bookingId(collection.getBooking().getId())
                .collectionId(collection.getId())
                .amount(collection.getAmount())
                .currency(collection.getCurrency())
                .qrCodeId(collection.getGatewayQrCodeId())
                .gatewayPaymentId(collection.getGatewayPaymentId())
                .imageUrl(collection.getGatewayQrImageUrl())
                .status(status)
                .createdAt(collection.getCreatedAt())
                .expiresAt(expiry)
                .build();
    }
}
