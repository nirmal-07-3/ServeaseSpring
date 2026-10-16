 package com.application.Servease.message.mapper;

import com.application.Servease.message.dto.MessageResponse;
import com.application.Servease.message.entity.Message;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageResponse toResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .bookingId(message.getBooking().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .recipientId(message.getRecipient().getId())
                .recipientName(message.getRecipient().getName())
                .content(message.getContent())
                .isRead(message.isRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
