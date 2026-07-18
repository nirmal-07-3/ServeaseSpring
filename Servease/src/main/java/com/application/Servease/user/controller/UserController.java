package com.application.Servease.user.controller;

import com.application.Servease.common.dto.ApiResponse;
import com.application.Servease.user.dto.ChangePasswordRequest;
import com.application.Servease.user.dto.UpdateProfileRequest;
import com.application.Servease.user.dto.UserProfileResponse;
import com.application.Servease.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser() {

        UserProfileResponse response = userService.getCurrentUser();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Profile fetched successfully",
                        response
                )
        );
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {

        UserProfileResponse response = userService.updateProfile(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Profile updated successfully",
                        response
                )
        );
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Password changed successfully",
                        null
                )
        );
    }
}