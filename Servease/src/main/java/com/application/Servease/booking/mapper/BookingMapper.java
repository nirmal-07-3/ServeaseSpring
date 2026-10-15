package com.application.Servease.booking.mapper;

import com.application.Servease.booking.dto.BookingResponse;
import com.application.Servease.booking.entity.Booking;
import com.application.Servease.booking.enums.PaymentPreference;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {
        LocalDateTime now = LocalDateTime.now();

        PaymentPreference effectivePreference =
                resolveEffectivePreference(booking);

        boolean deadlineValid =
                booking.getPaymentDeadlineAt() == null
                        || now.isBefore(booking.getPaymentDeadlineAt());

        boolean onlinePaymentAllowed =
                booking.getStatus().name().equals("ACCEPTED")
                        && effectivePreference == PaymentPreference.PAY_ONLINE
                        && deadlineValid;

        return BookingResponse.builder()
                .id(booking.getId())
                .customerId(booking.getCustomer().getId())
                .providerId(booking.getProviderService().getProvider().getId())
                .providerBusinessName(
                        booking.getProviderService().getProvider().getBusinessName())
                .providerServiceId(booking.getProviderService().getId())
                .serviceTitle(booking.getProviderService().getTitle())
                .subCategoryId(booking.getProviderService().getSubCategory().getId())
                .subCategoryName(
                        booking.getProviderService().getSubCategory().getName())
                .bookingDate(booking.getBookingDate())
                .bookingTime(booking.getBookingTime())
                .agreedPrice(booking.getAgreedPrice())
                .address(booking.getAddress())
                .city(booking.getCity())
                .state(booking.getState())
                .pincode(booking.getPincode())
                .customerNote(booking.getCustomerNote())
                .status(booking.getStatus())
                .paymentPreference(booking.getPaymentPreference())
                .effectivePaymentPreference(effectivePreference)
                .paymentDeadlineAt(booking.getPaymentDeadlineAt())
                .onlinePaymentAllowed(onlinePaymentAllowed)
                .estimatedDurationMinutesSnapshot(
                        booking.getEstimatedDurationMinutesSnapshot())
                .platformFeeRateSnapshot(booking.getPlatformFeeRateSnapshot())
                .platformFeeAmountSnapshot(booking.getPlatformFeeAmountSnapshot())
                .build();
    }

    private PaymentPreference resolveEffectivePreference(Booking booking) {
        String effective = booking.getEffectivePaymentPreference();

        if (effective != null && !effective.isBlank()) {
            try {
                return PaymentPreference.valueOf(effective);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        return booking.getPaymentPreference();
    }
}
