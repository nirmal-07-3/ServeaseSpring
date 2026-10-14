package com.application.Servease.message.repository;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.message.entity.Message;
import com.application.Servease.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository
        extends JpaRepository<Message, Long> {

    List<Message> findByBookingOrderByCreatedAtAsc(
            Booking booking
    );

    List<Message> findBySenderOrRecipientOrderByCreatedAtDesc(
            User sender,
            User recipient
    );
}