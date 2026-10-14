package com.application.Servease.booking.controller;

import com.application.Servease.booking.dto.BookingResponse;
import com.application.Servease.booking.service.BookingService;
import com.application.Servease.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provider/bookings")
@RequiredArgsConstructor
@Tag(
        name = "Provider - Bookings",
        description = "Provider booking management APIs"
)
public class ProviderBookingController {

    private final BookingService bookingService;

    @Operation(
            summary = "Get provider bookings"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookings() {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Provider bookings fetched successfully.",
                        bookingService.getProviderBookings()
                )
        );
    }

    @Operation(
            summary = "Get provider booking"
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Booking fetched successfully.",
                        bookingService.getProviderBooking(id)
                )
        );
    }

    @Operation(
            summary = "Accept booking"
    )
    @PatchMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptBooking(
            @PathVariable Long id) {

        bookingService.acceptBooking(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Booking accepted successfully.",
                        null
                )
        );
    }

    @Operation(
            summary = "Reject booking"
    )
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectBooking(
            @PathVariable Long id) {

        bookingService.rejectBooking(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Booking rejected successfully.",
                        null
                )
        );
    }

    @Operation(
            summary = "Legacy start endpoint disabled"
    )
    @PatchMapping("/{id}/start")
    public ResponseEntity<ApiResponse<Void>> startBooking(
            @PathVariable Long id
    ) {
        return ResponseEntity.status(409).body(
                new ApiResponse<>(
                        false,
                        "Request and verify a START code to begin the service.",
                        null
                )
        );
    }

    @Operation(
            summary = "Legacy completion endpoint disabled"
    )
    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<Void>> completeBooking(
            @PathVariable Long id
    ) {
        return ResponseEntity.status(409).body(
                new ApiResponse<>(
                        false,
                        "Confirm collection when applicable, then verify a COMPLETION code.",
                        null
                )
        );
    }
}