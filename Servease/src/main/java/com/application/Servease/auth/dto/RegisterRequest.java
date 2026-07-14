package com.application.Servease.auth.dto;

import com.application.Servease.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {


    private String name;
    private UserRole Role;
    private String email;
    private String password;
    private String phone;
    private String address;

}
