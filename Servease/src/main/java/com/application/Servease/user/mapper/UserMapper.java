package com.application.Servease.user.mapper;

import com.application.Servease.entity.User;
import com.application.Servease.user.dto.UserProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getProfileImage(),
                user.getRole(),
                user.getStatus(),
                user.getEmailVerified(),
                user.getPhoneVerified()
        );
    }
}