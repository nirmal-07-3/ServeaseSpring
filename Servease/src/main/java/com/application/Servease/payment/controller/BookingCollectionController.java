package com.application.Servease.payment.controller;

import com.application.Servease.common.dto.ApiResponse;
import com.application.Servease.payment.dto.ConfirmBookingCollectionRequest;
import com.application.Servease.payment.service.BookingCollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/provider/bookings")
@RequiredArgsConstructor
public class BookingCollectionController {

    private final BookingCollectionService collectionService;

    @PatchMapping("/{bookingId}/collection/confirm")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> confirmCollection(
            @PathVariable Long bookingId,
            @Valid @RequestBody ConfirmBookingCollectionRequest request
    ) {
        collectionService.confirmCollection(bookingId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Collection confirmation recorded.",
                        null
                )
        );
    }
}