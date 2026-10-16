package com.application.Servease.message.controller;

import com.application.Servease.common.dto.ApiResponse;
import com.application.Servease.message.dto.MessageResponse;
import com.application.Servease.message.dto.SendMessageRequest;
import com.application.Servease.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Tag(name = "Messages",
        description = "Customer and provider messaging APIs")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    @Operation(summary = "Send a message")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @Valid @RequestBody SendMessageRequest request) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Message sent successfully.",
                        messageService.sendMessage(request)
                )
        );
    }

    @GetMapping
    @Operation(summary = "Get my messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMyMessages() {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Messages fetched successfully.",
                        messageService.getMyMessages()
                )
        );
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get booking conversation")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getBookingMessages(
            @PathVariable Long bookingId) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Conversation fetched successfully.",
                        messageService.getBookingMessages(bookingId)
                )
        );
    }

    @PatchMapping("/booking/{bookingId}/read")
    @Operation(summary = "Mark received booking messages as read")
    public ResponseEntity<ApiResponse<Integer>> markBookingMessagesRead(
            @PathVariable Long bookingId) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Messages marked as read.",
                        messageService.markBookingMessagesRead(bookingId)
                )
        );
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread message count")
    public ResponseEntity<ApiResponse<Long>> getUnreadMessageCount() {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Unread message count fetched successfully.",
                        messageService.getUnreadMessageCount()
                )
        );
    }
}
