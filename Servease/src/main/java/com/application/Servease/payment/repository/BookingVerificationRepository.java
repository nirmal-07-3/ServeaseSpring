
package com.application.Servease.payment.repository;

import com.application.Servease.payment.entity.BookingVerification;
import com.application.Servease.payment.enums.VerificationPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingVerificationRepository
        extends JpaRepository<BookingVerification, Long> {

    List<BookingVerification> findByBookingIdAndPurposeOrderByCreatedAtDesc(
            Long bookingId,
            VerificationPurpose purpose
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select verification
            from BookingVerification verification
            where verification.booking.id = :bookingId
              and verification.purpose = :purpose
            order by verification.createdAt desc, verification.id desc
            """)
    List<BookingVerification> findForUpdateByBookingAndPurpose(
            @Param("bookingId") Long bookingId,
            @Param("purpose") VerificationPurpose purpose
    );

    Optional<BookingVerification>
    findTopByBookingIdAndPurposeOrderByCreatedAtDesc(
            Long bookingId,
            VerificationPurpose purpose
    );
}
