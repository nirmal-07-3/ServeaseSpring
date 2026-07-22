package com.application.Servease.user.service;

import com.application.Servease.common.exception.DuplicatePhoneException;
import com.application.Servease.common.exception.InvalidCredentialsException;
import com.application.Servease.common.exception.PasswordMismatchException;
import com.application.Servease.common.exception.UserNotFoundException;
import com.application.Servease.entity.User;
import com.application.Servease.repository.UserRepository;
import com.application.Servease.user.dto.ChangePasswordRequest;
import com.application.Servease.user.dto.UpdateProfileRequest;
import com.application.Servease.user.dto.UserProfileResponse;
import com.application.Servease.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    private User getAuthenticatedUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public UserProfileResponse getCurrentUser() {

        User user = getAuthenticatedUser();

        return userMapper.toProfileResponse(user);
    }

    public UserProfileResponse updateProfile(UpdateProfileRequest request) {

        User user = getAuthenticatedUser();

        userRepository.findByPhone(request.getPhone()).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(user.getId())) {
                throw new DuplicatePhoneException("Phone number already registered");
            }
        });

        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setProfileImage(request.getProfileImage());

        userRepository.save(user);

        return userMapper.toProfileResponse(user);
    }

    public void changePassword(ChangePasswordRequest request) {

        if (!Objects.equals(request.getNewPassword(), request.getConfirmNewPassword())) {
            throw new PasswordMismatchException("Confirm password must match the new password");
        }

        User user = getAuthenticatedUser();

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Old password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new PasswordMismatchException(
                    "New password cannot be the same as the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);
    }
}