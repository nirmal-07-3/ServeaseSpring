package com.application.Servease.auth.dto;

import com.application.Servease.entity.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@NotBlank
@Email
@Size
public class RegisterRequest {


    private String name;
    private UserRole role;
    private String email;
    private String password;
    private String phone;


}
