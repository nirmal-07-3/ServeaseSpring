package com.application.Servease.user.dto;

import com.application.Servease.user.enums.UserRole;
import com.application.Servease.user.enums.UserStatus;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        String phone,
        UserRole role,
        UserStatus status,
        Boolean emailVerified,
        Boolean phoneVerified,
        LocalDateTime createdAt
) {
}