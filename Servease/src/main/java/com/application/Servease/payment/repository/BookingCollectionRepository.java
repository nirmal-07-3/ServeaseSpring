package com.application.Servease.payment.repository;

import com.application.Servease.payment.entity.BookingCollection;
import com.application.Servease.payment.enums.CollectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingCollectionRepository extends JpaRepository<BookingCollection, Long> {

    Optional<BookingCollection> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    List<BookingCollection> findByStatusOrderByCreatedAtAsc(CollectionStatus status);
}