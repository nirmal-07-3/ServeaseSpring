package com.application.Servease.message.service;

import com.application.Servease.message.dto.MessageResponse;
import com.application.Servease.message.dto.SendMessageRequest;

import java.util.List;

public interface MessageService {

    MessageResponse sendMessage(SendMessageRequest request);

    List<MessageResponse> getMyMessages();

    List<MessageResponse> getBookingMessages(Long bookingId);

    int markBookingMessagesRead(Long bookingId);

    long getUnreadMessageCount();
}
