package com.application.Servease.auth.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private String email;



    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    String newPassword;

    @NotBlank(message = "Confirm password is required")
    String confirmNewPassword;

}
