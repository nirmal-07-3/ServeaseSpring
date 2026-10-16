
package com.application.Servease.booking.service;

import com.application.Servease.booking.dto.BookingResponse;
import com.application.Servease.booking.dto.CreateBookingRequest;
import com.application.Servease.booking.enums.BookingStatus;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request);

    List<BookingResponse> getMyBookings();

    BookingResponse getMyBooking(Long id);

    void cancelBooking(Long id);

    BookingResponse switchToPayAtService(Long id);

    List<BookingResponse> getProviderBookings();

    BookingResponse getProviderBooking(Long id);

    void acceptBooking(Long id);

    void rejectBooking(Long id);

    void startBooking(Long id);

    void completeBooking(Long id);
    BookingStatus getBookingStatusForMessaging(Long id);
}
