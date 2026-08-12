package com.application.Servease.booking.controller;

import com.application.Servease.booking.dto.BookingResponse;
import com.application.Servease.booking.dto.CreateBookingRequest;
import com.application.Servease.booking.service.BookingService;
import com.application.Servease.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(
        name = "Customer - Bookings",
        description = "Customer booking APIs"
)
public class BookingController {

    private final BookingService bookingService;

    @Operation(
            summary = "Create booking",
            description = "Create a new booking for an active provider service"
    )
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Booking created successfully.",
                        bookingService.createBooking(request)
                )
        );
    }

    @Operation(
            summary = "Get my bookings"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings() {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Bookings fetched successfully.",
                        bookingService.getMyBookings()
                )
        );
    }

    @Operation(
            summary = "Get my booking"
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getMyBooking(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Booking fetched successfully.",
                        bookingService.getMyBooking(id)
                )
        );
    }

    @Operation(
            summary = "Cancel booking"
    )
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @PathVariable Long id) {

        bookingService.cancelBooking(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Booking cancelled successfully.",
                        null
                )
        );
    }
}