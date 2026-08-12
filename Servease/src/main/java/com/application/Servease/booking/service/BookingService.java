package com.application.Servease.booking.service;

import com.application.Servease.booking.dto.BookingResponse;
import com.application.Servease.booking.dto.CreateBookingRequest;

import java.util.List;

public interface BookingService {

    // Customer
    BookingResponse createBooking(
            CreateBookingRequest request
    );

    List<BookingResponse> getMyBookings();

    BookingResponse getMyBooking(Long id);

    void cancelBooking(Long id);

    // Provider
    List<BookingResponse> getProviderBookings();

    BookingResponse getProviderBooking(Long id);

    void acceptBooking(Long id);

    void rejectBooking(Long id);

    void startBooking(Long id);

    void completeBooking(Long id);
}