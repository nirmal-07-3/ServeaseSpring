package com.application.Servease.message.repository;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.message.entity.Message;
import com.application.Servease.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByBookingOrderByCreatedAtAsc(Booking booking);

    List<Message> findBySenderOrRecipientOrderByCreatedAtDesc(
            User sender,
            User recipient
    );

    @Query("SELECT m FROM Message m WHERE m.booking = :booking AND m.recipient = :recipient AND m.isRead = false ORDER BY m.createdAt ASC")
    List<Message> findUnreadMessagesByBookingAndRecipient(
            @Param("booking") Booking booking,
            @Param("recipient") User recipient
    );

    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipient = :recipient AND m.isRead = false")
    long countUnreadMessagesByRecipient(
            @Param("recipient") User recipient
    );

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.booking = :booking AND m.recipient = :recipient AND m.isRead = false")
    int markMessagesAsReadByBookingAndRecipient(
            @Param("booking") Booking booking,
            @Param("recipient") User recipient
    );
}
