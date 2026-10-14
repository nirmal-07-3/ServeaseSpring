package com.application.Servease.booking.dto;

import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.booking.enums.PaymentPreference;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class BookingResponse {

    private Long id;
    private Long customerId;
    private Long providerId;
    private String providerBusinessName;
    private Long providerServiceId;
    private String serviceTitle;
    private Long subCategoryId;
    private String subCategoryName;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private BigDecimal agreedPrice;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String customerNote;
    private BookingStatus status;
    private PaymentPreference paymentPreference;
    private Integer estimatedDurationMinutesSnapshot;
    private BigDecimal platformFeeRateSnapshot;
    private BigDecimal platformFeeAmountSnapshot;
}
