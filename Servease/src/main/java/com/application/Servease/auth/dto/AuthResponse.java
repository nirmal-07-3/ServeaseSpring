package com.application.Servease.auth.dto;

import com.application.Servease.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private Long id;
    private String name;
    private String email;
    private UserRole role;
    private String token;
}

