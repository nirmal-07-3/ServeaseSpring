
package com.application.Servease.booking.repository;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.providerservice.entity.ProviderService;
import com.application.Servease.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerOrderByCreatedAtDesc(User customer);

    Optional<Booking> findByIdAndCustomer(Long id, User customer);

    List<Booking> findByProviderServiceOrderByCreatedAtDesc(
            ProviderService providerService
    );

    List<Booking> findByProviderServiceProviderOrderByCreatedAtDesc(
            ProviderProfile provider
    );

    Optional<Booking> findByIdAndProviderServiceProvider(
            Long id,
            ProviderProfile provider
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from Booking b
            where b.id = :bookingId
              and b.providerService.provider = :provider
            """)
    Optional<Booking> findByIdAndProviderServiceProviderForUpdate(
            @Param("bookingId") Long bookingId,
            @Param("provider") ProviderProfile provider
    );

    List<Booking> findByProviderServiceProviderAndBookingDateAndStatusIn(
            ProviderProfile provider,
            LocalDate bookingDate,
            List<BookingStatus> statuses
    );

    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);

    @Modifying
    @Query("""
            update Booking b
            set b.status = :cancelledStatus,
                b.cancellationReason = :reason,
                b.updatedAt = :now
            where b.status = :pendingStatus
              and b.providerResponseDeadlineAt is not null
              and b.providerResponseDeadlineAt <= :now
            """)
    int cancelExpiredPendingBookings(
            @Param("pendingStatus") BookingStatus pendingStatus,
            @Param("cancelledStatus") BookingStatus cancelledStatus,
            @Param("reason") String reason,
            @Param("now") LocalDateTime now
    );


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select b
        from Booking b
        where b.id = :bookingId
          and b.customer = :customer
        """)
    Optional<Booking> findByIdAndCustomerForUpdate(
            @Param("bookingId") Long bookingId,
            @Param("customer") User customer
    );

}


