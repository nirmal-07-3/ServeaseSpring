package com.application.Servease.booking.service;

import com.application.Servease.booking.dto.BookingResponse;
import com.application.Servease.booking.dto.CreateBookingRequest;
import com.application.Servease.booking.entity.Booking;
import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.booking.mapper.BookingMapper;
import com.application.Servease.booking.repository.BookingRepository;
import com.application.Servease.booking.service.BookingService;
import com.application.Servease.common.exception.*;
import com.application.Servease.provider.entity.ProviderAvailability;
import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.provider.enums.VerificationStatus;
import com.application.Servease.provider.repository.ProviderAvailabilityRepository;
import com.application.Servease.providerservice.entity.ProviderService;
import com.application.Servease.providerservice.repository.ProviderServiceRepository;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
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

    @Override
    public BookingResponse createBooking(
            CreateBookingRequest request) {

        // 1. Get authenticated customer
        User customer = userService.getAuthenticatedUser();

        // 2. Find active provider service
        ProviderService providerService =
                providerServiceRepository
                        .findByIdAndActiveTrue(
                                request.getProviderServiceId()
                        )
                        .orElseThrow(() ->
                                new BookingNotFoundException(
                                        "Active provider service not found."
                                ));

        // 3. Get provider from the service
        ProviderProfile provider =
                providerService.getProvider();

        // 4. Provider must be approved
        if (provider.getVerificationStatus()
                != VerificationStatus.APPROVED) {

            throw new BookingConflictException(
                    "This provider is not currently available for booking."
            );
        }

        // 5. Check provider availability
        DayOfWeek dayOfWeek =
                request.getBookingDate().getDayOfWeek();

        ProviderAvailability availability =
                providerAvailabilityRepository
                        .findByProviderAndDayOfWeek(
                                provider,
                                dayOfWeek
                        )
                        .orElseThrow(() ->
                                new BookingConflictException(
                                        "Provider has not configured availability for this day."
                                ));

        // 6. Check holiday
        if (!Boolean.TRUE.equals(
                availability.getAvailable())) {

            throw new BookingConflictException(
                    "Provider is not available on the selected day."
            );
        }

        // 7. Check working hours
        LocalTime bookingTime =
                request.getBookingTime();

        if (availability.getStartTime() == null
                || availability.getEndTime() == null) {

            throw new BookingConflictException(
                    "Provider has not configured working hours for this day."
            );
        }

        if (bookingTime.isBefore(
                availability.getStartTime())
                ||
                bookingTime.isAfter(
                        availability.getEndTime())) {

            throw new BookingConflictException(
                    "Selected time is outside the provider's working hours."
            );
        }

        // 8. Check existing active booking
        // 8. Check for overlapping active bookings

        List<BookingStatus> activeStatuses =
                List.of(
                        BookingStatus.PENDING,
                        BookingStatus.ACCEPTED,
                        BookingStatus.IN_PROGRESS
                );

        List<Booking> existingBookings =
                bookingRepository
                        .findByProviderServiceProviderAndBookingDateAndStatusIn(
                                provider,
                                request.getBookingDate(),
                                activeStatuses
                        );

        LocalTime requestedStart =
                request.getBookingTime();

        LocalTime requestedEnd =
                requestedStart.plusMinutes(
                        providerService
                                .getEstimatedDurationMinutes()
                );

        for (Booking existingBooking : existingBookings) {

            LocalTime existingStart =
                    existingBooking.getBookingTime();

            LocalTime existingEnd =
                    existingStart.plusMinutes(
                            existingBooking
                                    .getProviderService()
                                    .getEstimatedDurationMinutes()
                    );

            boolean overlaps =
                    requestedStart.isBefore(existingEnd)
                            &&
                            requestedEnd.isAfter(existingStart);

            if (overlaps) {

                throw new BookingConflictException(
                        "Provider already has a booking during the selected time."
                );
            }
        }

        // 9. Create booking
        Booking booking = Booking.builder()
                .customer(customer)
                .providerService(providerService)
                .bookingDate(request.getBookingDate())
                .bookingTime(request.getBookingTime())

                // Snapshot current service price
                .agreedPrice(
                        providerService.getBasePrice()
                )

                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .customerNote(request.getCustomerNote())

                .status(BookingStatus.PENDING)

                .build();

        Booking saved =
                bookingRepository.save(booking);

        return bookingMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings() {

        User customer = userService.getAuthenticatedUser();

        return bookingRepository
                .findByCustomerOrderByCreatedAtDesc(customer)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }



    @Override
    @Transactional(readOnly = true)
    public BookingResponse getMyBooking(Long id) {

        User customer = userService.getAuthenticatedUser();

        Booking booking =
                bookingRepository
                        .findByIdAndCustomer(id, customer)
                        .orElseThrow(() ->
                                new BookingNotFoundException(
                                        "Booking not found."
                                ));

        return bookingMapper.toResponse(booking);
    }


    @Override
    public void cancelBooking(Long id) {

        User customer = userService.getAuthenticatedUser();

        Booking booking =
                bookingRepository
                        .findByIdAndCustomer(id, customer)
                        .orElseThrow(() ->
                                new BookingNotFoundException(
                                        "Booking not found."
                                ));

        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.ACCEPTED) {

            throw new InvalidBookingStatusException(
                    "This booking cannot be cancelled in its current status."
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);

        bookingRepository.save(booking);
    }


    private ProviderProfile getAuthenticatedProvider() {

        User user = userService.getAuthenticatedUser();

        ProviderProfile provider = user.getProviderProfile();

        if (provider == null) {
            throw new ProviderNotFoundException(
                    "Provider profile not found."
            );
        }

        return provider;
    }


    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getProviderBookings() {

        ProviderProfile provider = getAuthenticatedProvider();

        return bookingRepository
                .findByProviderServiceProviderOrderByCreatedAtDesc(provider)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public BookingResponse getProviderBooking(Long id) {

        ProviderProfile provider = getAuthenticatedProvider();

        Booking booking =
                bookingRepository
                        .findByIdAndProviderServiceProvider(
                                id,
                                provider
                        )
                        .orElseThrow(() ->
                                new BookingNotFoundException(
                                        "Booking not found."
                                ));

        return bookingMapper.toResponse(booking);
    }


    @Override
    public void acceptBooking(Long id) {

        ProviderProfile provider = getAuthenticatedProvider();

        Booking booking =
                bookingRepository
                        .findByIdAndProviderServiceProvider(
                                id,
                                provider
                        )
                        .orElseThrow(() ->
                                new BookingNotFoundException(
                                        "Booking not found."
                                ));

        if (booking.getStatus() != BookingStatus.PENDING) {

            throw new InvalidBookingStatusException(
                    "Only pending bookings can be accepted."
            );
        }

        booking.setStatus(BookingStatus.ACCEPTED);

        bookingRepository.save(booking);
    }



    @Override
    public void rejectBooking(Long id) {

        ProviderProfile provider = getAuthenticatedProvider();

        Booking booking =
                bookingRepository
                        .findByIdAndProviderServiceProvider(
                                id,
                                provider
                        )
                        .orElseThrow(() ->
                                new BookingNotFoundException(
                                        "Booking not found."
                                ));

        if (booking.getStatus() != BookingStatus.PENDING) {

            throw new InvalidBookingStatusException(
                    "Only pending bookings can be rejected."
            );
        }

        booking.setStatus(BookingStatus.REJECTED);

        bookingRepository.save(booking);
    }


    @Override
    public void startBooking(Long id) {

        ProviderProfile provider = getAuthenticatedProvider();

        Booking booking =
                bookingRepository
                        .findByIdAndProviderServiceProvider(
                                id,
                                provider
                        )
                        .orElseThrow(() ->
                                new BookingNotFoundException(
                                        "Booking not found."
                                ));

        if (booking.getStatus() != BookingStatus.ACCEPTED) {

            throw new InvalidBookingStatusException(
                    "Only accepted bookings can be started."
            );
        }

        booking.setStatus(BookingStatus.IN_PROGRESS);

        bookingRepository.save(booking);
    }


    @Override
    public void completeBooking(Long id) {

        ProviderProfile provider = getAuthenticatedProvider();

        Booking booking =
                bookingRepository
                        .findByIdAndProviderServiceProvider(
                                id,
                                provider
                        )
                        .orElseThrow(() ->
                                new BookingNotFoundException(
                                        "Booking not found."
                                ));

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {

            throw new InvalidBookingStatusException(
                    "Only in-progress bookings can be completed."
            );
        }

        booking.setStatus(BookingStatus.COMPLETED);

        bookingRepository.save(booking);
    }
}