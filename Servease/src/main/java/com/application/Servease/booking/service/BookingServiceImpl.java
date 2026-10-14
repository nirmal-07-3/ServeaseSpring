
package com.application.Servease.booking.service;

import com.application.Servease.booking.dto.BookingResponse;
import com.application.Servease.booking.dto.CreateBookingRequest;
import com.application.Servease.booking.entity.Booking;
import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.booking.enums.PaymentPreference;
import com.application.Servease.booking.mapper.BookingMapper;
import com.application.Servease.booking.repository.BookingRepository;
import com.application.Servease.common.exception.*;
import com.application.Servease.payment.entity.BookingCollection;
import com.application.Servease.payment.entity.Payment;
import com.application.Servease.payment.entity.PaymentAttempt;
import com.application.Servease.payment.enums.CollectionStatus;
import com.application.Servease.payment.enums.PaymentAttemptStatus;
import com.application.Servease.payment.enums.PaymentStatus;
import com.application.Servease.payment.repository.BookingCollectionRepository;
import com.application.Servease.payment.repository.PaymentAttemptRepository;
import com.application.Servease.payment.repository.PaymentRepository;
import com.application.Servease.provider.entity.ProviderAvailability;
import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.provider.enums.VerificationStatus;
import com.application.Servease.provider.repository.ProviderAvailabilityRepository;
import com.application.Servease.providerservice.entity.ProviderService;
import com.application.Servease.providerservice.repository.ProviderServiceRepository;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ProviderServiceRepository providerServiceRepository;
    private final ProviderAvailabilityRepository providerAvailabilityRepository;
    private final BookingMapper bookingMapper;
    private final UserService userService;
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final BookingCollectionRepository bookingCollectionRepository;

    @Value("${servease.platform-fee-rate:10.00}")
    private BigDecimal platformFeeRate;

    @Value("${servease.booking.provider-response-timeout-minutes:15}")
    private long providerResponseTimeoutMinutes;

    @Value("${servease.booking.online-payment-timeout-minutes:30}")
    private long onlinePaymentTimeoutMinutes;

    @Override
    public BookingResponse createBooking(CreateBookingRequest request) {
        User customer = userService.getAuthenticatedUser();

        if (request.getPaymentPreference() == null) {
            throw new BookingConflictException("Payment preference is required.");
        }

        if (request.getBookingDate() == null || request.getBookingTime() == null) {
            throw new BookingConflictException("Booking date and time are required.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentStart = LocalDateTime.of(
                request.getBookingDate(),
                request.getBookingTime()
        );

        if (!appointmentStart.isAfter(now)) {
            throw new BookingConflictException(
                    "The booking date and time must be in the future."
            );
        }

        if (providerResponseTimeoutMinutes <= 0 || onlinePaymentTimeoutMinutes <= 0) {
            throw new IllegalStateException(
                    "Booking deadline settings must be greater than zero."
            );
        }

        ProviderService providerService = providerServiceRepository
                .findByIdAndActiveTrue(request.getProviderServiceId())
                .orElseThrow(() -> new BookingNotFoundException(
                        "Active provider service not found."
                ));

        ProviderProfile provider = providerService.getProvider();

        if (provider.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new BookingConflictException(
                    "This provider is not currently available for booking."
            );
        }

        Integer durationMinutes = providerService.getEstimatedDurationMinutes();

        if (durationMinutes == null || durationMinutes <= 0) {
            throw new BookingConflictException(
                    "The service has an invalid estimated duration."
            );
        }

        BigDecimal agreedPrice = providerService.getBasePrice();

        if (agreedPrice == null || agreedPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BookingConflictException("The service has an invalid price.");
        }

        if (platformFeeRate == null
                || platformFeeRate.compareTo(BigDecimal.ZERO) < 0
                || platformFeeRate.compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalStateException(
                    "Platform fee rate must be between 0 and 100."
            );
        }

        BigDecimal feeRateSnapshot =
                platformFeeRate.setScale(2, RoundingMode.HALF_UP);

        BigDecimal feeAmountSnapshot = agreedPrice
                .multiply(feeRateSnapshot)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        ProviderAvailability availability = providerAvailabilityRepository
                .findByProviderAndDayOfWeek(
                        provider,
                        request.getBookingDate().getDayOfWeek()
                )
                .orElseThrow(() -> new BookingConflictException(
                        "Provider has not configured availability for this day."
                ));

        if (!Boolean.TRUE.equals(availability.getAvailable())) {
            throw new BookingConflictException(
                    "Provider is not available on the selected day."
            );
        }

        LocalTime requestedStart = request.getBookingTime();

        if (availability.getStartTime() == null || availability.getEndTime() == null) {
            throw new BookingConflictException(
                    "Provider has not configured working hours for this day."
            );
        }

        LocalTime requestedEnd = requestedStart.plusMinutes(durationMinutes);

        if (requestedStart.isBefore(availability.getStartTime())
                || requestedEnd.isAfter(availability.getEndTime())) {
            throw new BookingConflictException(
                    "The full service duration must fit within the provider's working hours."
            );
        }

        List<BookingStatus> activeStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.ACCEPTED,
                BookingStatus.IN_PROGRESS
        );

        List<Booking> existingBookings = bookingRepository
                .findByProviderServiceProviderAndBookingDateAndStatusIn(
                        provider,
                        request.getBookingDate(),
                        activeStatuses
                );

        for (Booking existingBooking : existingBookings) {
            LocalTime existingStart = existingBooking.getBookingTime();

            Integer existingDuration =
                    existingBooking.getEstimatedDurationMinutesSnapshot();

            if (existingDuration == null) {
                existingDuration = existingBooking.getProviderService()
                        .getEstimatedDurationMinutes();
            }

            if (existingDuration == null || existingDuration <= 0) {
                throw new BookingConflictException(
                        "An existing booking has an invalid duration. Please contact support."
                );
            }

            LocalTime existingEnd = existingStart.plusMinutes(existingDuration);

            boolean overlaps = requestedStart.isBefore(existingEnd)
                    && requestedEnd.isAfter(existingStart);

            if (overlaps) {
                throw new BookingConflictException(
                        "Provider already has a booking during the selected time."
                );
            }
        }

        LocalDateTime responseDeadline =
                now.plusMinutes(providerResponseTimeoutMinutes);

        if (!responseDeadline.isBefore(appointmentStart)) {
            responseDeadline = appointmentStart;
        }

        Booking booking = Booking.builder()
                .customer(customer)
                .providerService(providerService)
                .bookingDate(request.getBookingDate())
                .bookingTime(request.getBookingTime())
                .agreedPrice(agreedPrice)
                .paymentPreference(request.getPaymentPreference())
                .estimatedDurationMinutesSnapshot(durationMinutes)
                .platformFeeRateSnapshot(feeRateSnapshot)
                .platformFeeAmountSnapshot(feeAmountSnapshot)
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .customerNote(request.getCustomerNote())
                .status(BookingStatus.PENDING)
                .providerResponseDeadlineAt(responseDeadline)
                .effectivePaymentPreference(request.getPaymentPreference().name())
                .build();

        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings() {
        User customer = userService.getAuthenticatedUser();

        return bookingRepository.findByCustomerOrderByCreatedAtDesc(customer)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getMyBooking(Long id) {
        User customer = userService.getAuthenticatedUser();

        Booking booking = bookingRepository.findByIdAndCustomer(id, customer)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found."));

        return bookingMapper.toResponse(booking);
    }

    @Override
    public void cancelBooking(Long id) {
        User customer = userService.getAuthenticatedUser();

        Booking booking = bookingRepository.findByIdAndCustomerForUpdate(id, customer)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found."));

        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new InvalidBookingStatusException(
                    "This booking cannot be cancelled in its current status."
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason("CUSTOMER_CANCELLED");
        bookingRepository.save(booking);
    }

    @Override
    public BookingResponse switchToPayAtService(Long id) {
        User customer = userService.getAuthenticatedUser();

        Booking booking = bookingRepository
                .findByIdAndCustomerForUpdate(id, customer)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found."));

        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new InvalidBookingStatusException(
                    "Payment preference can only be switched for an accepted booking."
            );
        }

        if (booking.getPaymentPreference() != PaymentPreference.PAY_ONLINE) {
            throw new InvalidBookingStatusException(
                    "Only bookings originally configured for online payment can use this fallback."
            );
        }

        String effectivePreference = booking.getEffectivePaymentPreference();

        if (effectivePreference != null
                && !effectivePreference.isBlank()
                && !PaymentPreference.PAY_ONLINE.name().equals(effectivePreference)) {
            throw new InvalidBookingStatusException(
                    "This booking's payment preference has already been changed."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        if (booking.getPaymentDeadlineAt() == null
                || now.isBefore(booking.getPaymentDeadlineAt())) {
            throw new InvalidBookingStatusException(
                    "The online payment deadline has not expired yet."
            );
        }

        LocalDateTime appointmentStart = LocalDateTime.of(
                booking.getBookingDate(),
                booking.getBookingTime()
        );

        if (!appointmentStart.isAfter(now)) {
            throw new InvalidBookingStatusException(
                    "Payment fallback is unavailable after the appointment start time."
            );
        }

        Payment payment = paymentRepository.findByBookingId(id).orElse(null);

        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new InvalidBookingStatusException(
                    "A successful online payment already exists. Do not switch or pay again."
            );
        }

        BookingCollection collection = bookingCollectionRepository
                .findByBookingId(id)
                .orElse(null);

        if (collection != null && collection.getStatus() == CollectionStatus.CONFIRMED) {
            throw new InvalidBookingStatusException(
                    "A collection has already been confirmed for this booking."
            );
        }

        List<PaymentAttempt> attempts =
                paymentAttemptRepository.findByBookingIdOrderByAttemptNumberDesc(id);

        for (PaymentAttempt attempt : attempts) {
            PaymentAttemptStatus status = attempt.getStatus();

            if (status == PaymentAttemptStatus.CAPTURED
                    || status == PaymentAttemptStatus.RECONCILIATION_REQUIRED) {
                throw new InvalidBookingStatusException(
                        "A payment attempt needs reconciliation. Contact support before switching payment methods."
                );
            }

            boolean gatewayOrderMayStillBePayable =
                    attempt.getGatewayOrderId() != null
                            && !attempt.getGatewayOrderId().isBlank()
                            && (status == PaymentAttemptStatus.CREATED
                            || status == PaymentAttemptStatus.PENDING
                            || status == PaymentAttemptStatus.AUTHORIZED);

            if (gatewayOrderMayStillBePayable) {
                throw new InvalidBookingStatusException(
                        "An existing online payment order may still be payable. Resolve its status before switching to payment at service."
                );
            }
        }

        booking.setEffectivePaymentPreference(PaymentPreference.PAY_AT_SERVICE.name());
        booking.setPaymentPreferenceSwitchedAt(now);

        Booking savedBooking = bookingRepository.saveAndFlush(booking);
        return bookingMapper.toResponse(savedBooking);
    }

    private ProviderProfile getAuthenticatedProvider() {
        User user = userService.getAuthenticatedUser();
        ProviderProfile provider = user.getProviderProfile();

        if (provider == null || provider.getId() == null) {
            throw new ProviderNotFoundException("Provider profile not found.");
        }

        return provider;
    }

    private Booking getProviderBookingForUpdate(Long id, ProviderProfile provider) {
        return bookingRepository.findByIdAndProviderServiceProviderForUpdate(id, provider)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getProviderBookings() {
        ProviderProfile provider = getAuthenticatedProvider();

        return bookingRepository.findByProviderServiceProviderOrderByCreatedAtDesc(provider)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getProviderBooking(Long id) {
        ProviderProfile provider = getAuthenticatedProvider();

        Booking booking = bookingRepository.findByIdAndProviderServiceProvider(id, provider)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found."));

        return bookingMapper.toResponse(booking);
    }

    @Override
    public void acceptBooking(Long id) {
        Booking booking = getProviderBookingForUpdate(id, getAuthenticatedProvider());

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidBookingStatusException(
                    "Only pending bookings can be accepted."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime responseDeadline = booking.getProviderResponseDeadlineAt();

        if (responseDeadline != null && !now.isBefore(responseDeadline)) {
            throw new InvalidBookingStatusException(
                    "The provider acceptance deadline has passed. This booking must expire."
            );
        }

        LocalDateTime appointmentStart = LocalDateTime.of(
                booking.getBookingDate(),
                booking.getBookingTime()
        );

        booking.setStatus(BookingStatus.ACCEPTED);
        booking.setProviderAcceptedAt(now);

        if (booking.getPaymentPreference() == PaymentPreference.PAY_ONLINE) {
            LocalDateTime paymentDeadline =
                    now.plusMinutes(onlinePaymentTimeoutMinutes);

            if (paymentDeadline.isAfter(appointmentStart)) {
                paymentDeadline = appointmentStart;
            }

            booking.setPaymentDeadlineAt(paymentDeadline);
        } else {
            booking.setPaymentDeadlineAt(null);
        }

        bookingRepository.save(booking);
    }

    @Override
    public void rejectBooking(Long id) {
        Booking booking = getProviderBookingForUpdate(id, getAuthenticatedProvider());

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidBookingStatusException(
                    "Only pending bookings can be rejected."
            );
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setCancellationReason("PROVIDER_REJECTED");
        bookingRepository.save(booking);
    }

    @Override
    public void startBooking(Long id) {
        throw new InvalidBookingStatusException(
                "Direct service start is disabled. Request and verify a START code."
        );
    }

    @Override
    public void completeBooking(Long id) {
        throw new InvalidBookingStatusException(
                "Direct service completion is disabled. Confirm collection when applicable, then verify a COMPLETION code."
        );
    }
}
