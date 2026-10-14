
package com.application.Servease.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BookingVerificationRequest {

    @NotBlank(message = "Verification code is required.")
    @Pattern(
            regexp = "\\d{6}",
            message = "Verification code must contain exactly 6 digits."
    )
    private String code;
}
