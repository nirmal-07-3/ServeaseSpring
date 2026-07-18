package com.application.Servease.user.dto;

import com.application.Servease.entity.enums.UserRole;
import com.application.Servease.entity.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String profileImage;
    private UserRole role;
    private UserStatus status;
    private Boolean emailVerified;
    private Boolean phoneVerified;
}