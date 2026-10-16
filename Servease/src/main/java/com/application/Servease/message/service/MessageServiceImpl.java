package com.application.Servease.message.service;

import com.application.Servease.booking.entity.Booking;
import com.application.Servease.booking.repository.BookingRepository;
import com.application.Servease.common.exception.BookingNotFoundException;
import com.application.Servease.common.exception.UserNotFoundException;
import com.application.Servease.message.dto.MessageResponse;
import com.application.Servease.message.dto.SendMessageRequest;
import com.application.Servease.message.entity.Message;
import com.application.Servease.message.mapper.MessageMapper;
import com.application.Servease.message.repository.MessageRepository;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageServiceImpl implements MessageService {

    private static final Set<String> MESSAGING_ALLOWED_STATUSES = Set.of(
            "PENDING",
            "ACCEPTED",
            "IN_PROGRESS"
    );

    private final MessageRepository messageRepository;
    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final MessageMapper messageMapper;

    @Override
    public MessageResponse sendMessage(SendMessageRequest request) {
        User currentUser = userService.getAuthenticatedUser();

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() ->
                        new BookingNotFoundException("Booking not found."));

        User recipient = getRecipient(booking, currentUser);

        String content = request.getContent() == null
                ? ""
                : request.getContent().trim();

        if (content.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message cannot be empty."
            );
        }

        if (content.length() > 2000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Messages cannot exceed 2000 characters."
            );
        }

        String bookingStatus = booking.getStatus() == null
                ? ""
                : booking.getStatus().name();

        if (!MESSAGING_ALLOWED_STATUSES.contains(bookingStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Messaging is closed for this booking. You can still view its conversation history."
            );
        }

        Message message = Message.builder()
                .booking(booking)
                .sender(currentUser)
                .recipient(recipient)
                .content(content)
                .isRead(false)
                .build();

        return messageMapper.toResponse(messageRepository.save(message));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getMyMessages() {
        User currentUser = userService.getAuthenticatedUser();

        return messageRepository
                .findBySenderOrRecipientOrderByCreatedAtDesc(
                        currentUser,
                        currentUser
                )
                .stream()
                .map(messageMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getBookingMessages(Long bookingId) {
        User currentUser = userService.getAuthenticatedUser();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new BookingNotFoundException("Booking not found."));

        verifyAccess(booking, currentUser);

        return messageRepository
                .findByBookingOrderByCreatedAtAsc(booking)
                .stream()
                .map(messageMapper::toResponse)
                .toList();
    }

    @Override
    public int markBookingMessagesRead(Long bookingId) {
        User currentUser = userService.getAuthenticatedUser();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new BookingNotFoundException("Booking not found."));

        verifyAccess(booking, currentUser);

        List<Message> unreadMessages =
                messageRepository.findUnreadMessagesByBookingAndRecipient(
                        booking,
                        currentUser
                );

        unreadMessages.forEach(message -> message.setRead(true));

        messageRepository.saveAll(unreadMessages);

        return unreadMessages.size();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadMessageCount() {
        User currentUser = userService.getAuthenticatedUser();

        return messageRepository.countUnreadMessagesByRecipient(currentUser);
    }

    private User getRecipient(Booking booking, User currentUser) {
        User customer = booking.getCustomer();

        User providerUser = booking.getProviderService()
                .getProvider()
                .getUser();

        if (customer.getId().equals(currentUser.getId())) {
            return providerUser;
        }

        if (providerUser.getId().equals(currentUser.getId())) {
            return customer;
        }

        throw new UserNotFoundException(
                "You are not part of this booking."
        );
    }

    private void verifyAccess(Booking booking, User currentUser) {
        User customer = booking.getCustomer();

        User providerUser = booking.getProviderService()
                .getProvider()
                .getUser();

        boolean isCustomer =
                customer.getId().equals(currentUser.getId());

        boolean isProvider =
                providerUser.getId().equals(currentUser.getId());

        if (!isCustomer && !isProvider) {
            throw new UserNotFoundException(
                    "You are not part of this booking."
            );
        }
    }
}
