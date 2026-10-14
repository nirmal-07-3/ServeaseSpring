
package com.application.Servease.payment.service;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.booking.enums.PaymentPreference;
import com.application.Servease.booking.repository.BookingRepository;
import com.application.Servease.common.exception.BookingNotFoundException;
import com.application.Servease.common.exception.InvalidBookingStatusException;
import com.application.Servease.common.exception.ProviderNotFoundException;
import com.application.Servease.common.service.EmailService;
import com.application.Servease.payment.dto.BookingVerificationRequest;
import com.application.Servease.payment.entity.BookingCollection;
import com.application.Servease.payment.entity.BookingVerification;
import com.application.Servease.payment.entity.Payment;
import com.application.Servease.payment.enums.CollectionStatus;
import com.application.Servease.payment.enums.PaymentStatus;
import com.application.Servease.payment.enums.VerificationPurpose;
import com.application.Servease.payment.repository.BookingCollectionRepository;
import com.application.Servease.payment.repository.BookingVerificationRepository;
import com.application.Servease.payment.repository.PaymentRepository;
import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class BookingVerificationService {

    private static final int OTP_VALIDITY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final BookingRepository bookingRepository;
    private final BookingVerificationRepository verificationRepository;
    private final BookingCollectionRepository collectionRepository;
    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${servease.booking.verification.enabled:true}")
    private boolean verificationEnabled;

    public BookingVerificationService(
            BookingRepository bookingRepository,
            BookingVerificationRepository verificationRepository,
            BookingCollectionRepository collectionRepository,
            PaymentRepository paymentRepository,
            UserService userService,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            PlatformTransactionManager transactionManager
    ) {
        this.bookingRepository = bookingRepository;
        this.verificationRepository = verificationRepository;
        this.collectionRepository = collectionRepository;
        this.paymentRepository = paymentRepository;
        this.userService = userService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;

        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRED
        );
    }

    @Transactional
    public void requestCode(Long bookingId, VerificationPurpose purpose) {
        ensureVerificationEnabled();
        validatePurpose(purpose);

        User providerUser = userService.getAuthenticatedUser();
        Booking booking = getProviderBookingForUpdate(bookingId, providerUser);

        validateRequestState(booking, purpose);
        validatePaymentCondition(booking, purpose);

        LocalDateTime now = LocalDateTime.now();

        List<BookingVerification> existing =
                verificationRepository.findForUpdateByBookingAndPurpose(
                        bookingId,
                        purpose
                );

        BookingVerification latest = getLatest(existing);

        if (latest != null
                && latest.getConsumedAt() == null
                && latest.getInvalidatedAt() == null
                && latest.getExpiresAt().isAfter(now)
                && latest.getResendAvailableAt() != null
                && latest.getResendAvailableAt().isAfter(now)) {
            throw new InvalidBookingStatusException(
                    "Please wait before requesting another verification code."
            );
        }

        invalidateUnusedCodes(existing, now);

        User customer = booking.getCustomer();

        if (customer == null
                || customer.getEmail() == null
                || customer.getEmail().isBlank()) {
            throw new InvalidBookingStatusException(
                    "The customer does not have a usable email address."
            );
        }

        String otp = generateOtp();

        BookingVerification verification = BookingVerification.builder()
                .booking(booking)
                .purpose(purpose)
                .codeHash(passwordEncoder.encode(otp))
                .expiresAt(now.plusMinutes(OTP_VALIDITY_MINUTES))
                .attemptsUsed(0)
                .maxAttempts(MAX_ATTEMPTS)
                .resendAvailableAt(now.plusSeconds(RESEND_COOLDOWN_SECONDS))
                .createdBy(providerUser)
                .build();

        verificationRepository.saveAndFlush(verification);

        emailService.sendBookingVerificationOtpEmail(
                customer.getEmail(),
                otp,
                bookingId,
                purpose.name()
        );
    }

    public void verifyCode(
            Long bookingId,
            VerificationPurpose purpose,
            BookingVerificationRequest request
    ) {
        ensureVerificationEnabled();
        validatePurpose(purpose);

        if (request == null
                || request.getCode() == null
                || !request.getCode().matches("\\d{6}")) {
            throw new InvalidBookingStatusException(
                    "A valid 6-digit verification code is required."
            );
        }

        String error = transactionTemplate.execute(status ->
                verifyCodeInTransaction(bookingId, purpose, request)
        );

        if (error != null) {
            throw new InvalidBookingStatusException(error);
        }
    }

    private String verifyCodeInTransaction(
            Long bookingId,
            VerificationPurpose purpose,
            BookingVerificationRequest request
    ) {
        User providerUser = userService.getAuthenticatedUser();

        Booking booking = getProviderBookingForUpdate(
                bookingId,
                providerUser
        );

        validateRequestState(booking, purpose);
        validatePaymentCondition(booking, purpose);

        List<BookingVerification> verifications =
                verificationRepository.findForUpdateByBookingAndPurpose(
                        bookingId,
                        purpose
                );

        BookingVerification verification = getLatest(verifications);

        if (verification == null) {
            return "Request a verification code first.";
        }

        LocalDateTime now = LocalDateTime.now();

        if (verification.getConsumedAt() != null
                || verification.getInvalidatedAt() != null) {
            return "This verification code is no longer valid.";
        }

        if (!verification.getExpiresAt().isAfter(now)) {
            verification.setInvalidatedAt(now);
            verificationRepository.saveAndFlush(verification);
            return "The verification code has expired. Request a new code.";
        }

        if (verification.getAttemptsUsed() >= verification.getMaxAttempts()) {
            verification.setInvalidatedAt(now);
            verificationRepository.saveAndFlush(verification);
            return "Maximum verification attempts reached. Request a new code later.";
        }

        verification.setAttemptsUsed(verification.getAttemptsUsed() + 1);

        if (!passwordEncoder.matches(
                request.getCode(),
                verification.getCodeHash())) {

            if (verification.getAttemptsUsed() >= verification.getMaxAttempts()) {
                verification.setInvalidatedAt(now);
            }

            verificationRepository.saveAndFlush(verification);
            return "Incorrect verification code.";
        }

        verification.setConsumedAt(now);
        verificationRepository.saveAndFlush(verification);

        if (purpose == VerificationPurpose.START) {
            booking.setStatus(BookingStatus.IN_PROGRESS);
        } else {
            booking.setStatus(BookingStatus.COMPLETED);
        }

        bookingRepository.saveAndFlush(booking);

        return null;
    }

    private Booking getProviderBookingForUpdate(
            Long bookingId,
            User providerUser
    ) {
        ProviderProfile provider = providerUser.getProviderProfile();

        if (provider == null || provider.getId() == null) {
            throw new ProviderNotFoundException("Provider profile not found.");
        }

        return bookingRepository
                .findByIdAndProviderServiceProviderForUpdate(
                        bookingId,
                        provider
                )
                .orElseThrow(() ->
                        new BookingNotFoundException("Booking not found."));
    }

    private BookingVerification getLatest(
            List<BookingVerification> verifications
    ) {
        return verifications.stream()
                .max(
                        Comparator
                                .comparing(BookingVerification::getCreatedAt)
                                .thenComparing(BookingVerification::getId)
                )
                .orElse(null);
    }

    private void validateRequestState(
            Booking booking,
            VerificationPurpose purpose
    ) {
        if (purpose == VerificationPurpose.START
                && booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new InvalidBookingStatusException(
                    "A start verification code can only be used for an accepted booking."
            );
        }

        if (purpose == VerificationPurpose.COMPLETION
                && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new InvalidBookingStatusException(
                    "A completion verification code can only be used for an in-progress booking."
            );
        }
    }

    private void validatePaymentCondition(
            Booking booking,
            VerificationPurpose purpose
    ) {
        String effectivePreference = booking.getEffectivePaymentPreference();

        if (effectivePreference == null || effectivePreference.isBlank()) {
            PaymentPreference originalPreference = booking.getPaymentPreference();

            if (originalPreference == null) {
                throw new InvalidBookingStatusException(
                        "Payment preference is missing. Contact support."
                );
            }

            effectivePreference = originalPreference.name();
        }

        if ("PAY_ONLINE".equals(effectivePreference)) {
            Payment payment = paymentRepository
                    .findByBookingId(booking.getId())
                    .orElseThrow(() -> new InvalidBookingStatusException(
                            "Online payment must be completed before service verification."
                    ));

            if (payment.getStatus() != PaymentStatus.SUCCESS) {
                throw new InvalidBookingStatusException(
                        "Online payment must be successful before service verification."
                );
            }
        } else if (!"PAY_AT_SERVICE".equals(effectivePreference)) {
            throw new InvalidBookingStatusException(
                    "The booking has an unsupported effective payment preference."
            );
        }

        if (purpose == VerificationPurpose.COMPLETION
                && "PAY_AT_SERVICE".equals(effectivePreference)) {
            BookingCollection collection = collectionRepository
                    .findByBookingId(booking.getId())
                    .orElseThrow(() -> new InvalidBookingStatusException(
                            "Confirm the customer's service payment before completing the booking."
                    ));

            if (collection.getStatus() != CollectionStatus.CONFIRMED) {
                throw new InvalidBookingStatusException(
                        "Service payment must be confirmed before completion."
                );
            }
        }
    }

    private void invalidateUnusedCodes(
            List<BookingVerification> verifications,
            LocalDateTime now
    ) {
        for (BookingVerification verification : verifications) {
            if (verification.getConsumedAt() == null
                    && verification.getInvalidatedAt() == null) {
                verification.setInvalidatedAt(now);
            }
        }

        verificationRepository.saveAllAndFlush(verifications);
    }

    private String generateOtp() {
        return "%06d".formatted(secureRandom.nextInt(1_000_000));
    }

    private void validatePurpose(VerificationPurpose purpose) {
        if (purpose == null) {
            throw new InvalidBookingStatusException(
                    "A valid verification purpose is required."
            );
        }
    }

    private void ensureVerificationEnabled() {
        if (!verificationEnabled) {
            throw new InvalidBookingStatusException(
                    "Booking verification is temporarily unavailable."
            );
        }
    }
}
