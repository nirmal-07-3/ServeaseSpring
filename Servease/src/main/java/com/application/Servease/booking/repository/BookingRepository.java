package com.application.Servease.booking.repository;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.providerservice.entity.ProviderService;
import com.application.Servease.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository
        extends JpaRepository<Booking, Long> {

    /*
     * Get all bookings of a customer.
     */
    List<Booking> findByCustomerOrderByCreatedAtDesc(
            User customer
    );

    /*
     * Find a particular booking belonging to a customer.
     */
    Optional<Booking> findByIdAndCustomer(
            Long id,
            User customer
    );

    /*
     * Get bookings for a particular provider service.
     */
    List<Booking> findByProviderServiceOrderByCreatedAtDesc(
            ProviderService providerService
    );

    /*
     * Get all bookings belonging to a provider.
     *
     * Provider is derived through ProviderService.
     */
    List<Booking> findByProviderServiceProviderOrderByCreatedAtDesc(
            ProviderProfile provider
    );

    /*
     * Find a booking belonging to a provider.
     */
    Optional<Booking> findByIdAndProviderServiceProvider(
            Long id,
            ProviderProfile provider
    );

    /*
     * Check whether a provider has an active booking
     * at the requested date and time.
     *
     * We will use this for double-booking prevention.
     */
    List<Booking> findByProviderServiceProviderAndBookingDateAndStatusIn(
            ProviderProfile provider,
            LocalDate bookingDate,
            List<BookingStatus> statuses
    );

    /*
     * Get all bookings with a particular status.
     *
     * Useful for admin dashboards later.
     */
    List<Booking> findByStatusOrderByCreatedAtDesc(
            BookingStatus status
    );
}