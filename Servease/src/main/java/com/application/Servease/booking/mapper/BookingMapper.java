package com.application.Servease.booking.mapper;

import com.application.Servease.booking.dto.BookingResponse;
import com.application.Servease.booking.entity.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {

        return BookingResponse.builder()
                .id(booking.getId())

                .customerId(
                        booking.getCustomer().getId()
                )

                .providerId(
                        booking.getProviderService()
                                .getProvider()
                                .getId()
                )

                .providerBusinessName(
                        booking.getProviderService()
                                .getProvider()
                                .getBusinessName()
                )

                .providerServiceId(
                        booking.getProviderService().getId()
                )

                .serviceTitle(
                        booking.getProviderService().getTitle()
                )

                .subCategoryId(
                        booking.getProviderService()
                                .getSubCategory()
                                .getId()
                )

                .subCategoryName(
                        booking.getProviderService()
                                .getSubCategory()
                                .getName()
                )

                .bookingDate(booking.getBookingDate())
                .bookingTime(booking.getBookingTime())
                .agreedPrice(booking.getAgreedPrice())
                .address(booking.getAddress())
                .city(booking.getCity())
                .state(booking.getState())
                .pincode(booking.getPincode())
                .customerNote(booking.getCustomerNote())
                .status(booking.getStatus())

                .build();
    }
}