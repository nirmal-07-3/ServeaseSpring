package com.application.Servease.booking.dto;

import com.application.Servease.booking.enums.BookingStatus;
import com.application.Servease.booking.enums.PaymentPreference;
import com.application.Servease.payment.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@Getter
@Setter
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
    private PaymentPreference effectivePaymentPreference;
    private LocalDateTime paymentDeadlineAt;
    private boolean onlinePaymentAllowed;
    private PaymentStatus paymentStatus;
    private boolean paymentReconciliationRequired;
    private Integer estimatedDurationMinutesSnapshot;
    private BigDecimal platformFeeRateSnapshot;
    private BigDecimal platformFeeAmountSnapshot;
}
