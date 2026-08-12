package com.application.Servease.booking.dto;

import com.application.Servease.booking.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateBookingStatusRequest {

    @NotNull(message = "Booking status is required")
    private BookingStatus status;
}