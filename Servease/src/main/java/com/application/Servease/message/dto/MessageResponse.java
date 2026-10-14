package com.application.Servease.message.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponse {

    private Long id;

    private Long bookingId;

    private Long senderId;

    private String senderName;

    private Long recipientId;

    private String recipientName;

    private String content;

    private LocalDateTime createdAt;
}