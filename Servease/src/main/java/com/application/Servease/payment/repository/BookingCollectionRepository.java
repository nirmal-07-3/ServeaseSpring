
package com.application.Servease.payment.repository;

import com.application.Servease.payment.entity.BookingCollection;
import com.application.Servease.payment.enums.CollectionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BookingCollectionRepository extends JpaRepository<BookingCollection, Long> {

    Optional<BookingCollection> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    Optional<BookingCollection> findByGatewayQrCodeId(String gatewayQrCodeId);

    Optional<BookingCollection> findByGatewayPaymentId(String gatewayPaymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BookingCollection c where c.booking.id = :bookingId")
    Optional<BookingCollection> findByBookingIdForUpdate(@Param("bookingId") Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BookingCollection c where c.gatewayQrCodeId = :qrCodeId")
    Optional<BookingCollection> findByGatewayQrCodeIdForUpdate(@Param("qrCodeId") String qrCodeId);

    List<BookingCollection> findByStatusOrderByCreatedAtAsc(CollectionStatus status);

    @Query("""
        select coalesce(sum(c.platformFeeAmountSnapshot), 0)
        from BookingCollection c
        where c.status = :status
        """)
    BigDecimal sumPlatformFeesByStatus(
            @Param("status") CollectionStatus status
    );

}
