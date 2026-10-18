package com.application.Servease.user.controller;

import com.application.Servease.common.dto.ApiResponse;
import com.application.Servease.user.dto.AdminUserResponse;
import com.application.Servease.user.dto.UpdateUserStatusRequest;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.enums.UserRole;
import com.application.Servease.user.enums.UserStatus;
import com.application.Servease.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status) {

        String term = search == null
                ? ""
                : search.trim().toLowerCase(Locale.ROOT);

        List<AdminUserResponse> users = userRepository.findAll()
                .stream()
                .filter(user -> role == null || user.getRole() == role)
                .filter(user -> status == null || user.getStatus() == status)
                .filter(user -> term.isBlank()
                        || contains(user.getName(), term)
                        || contains(user.getEmail(), term)
                        || contains(user.getPhone(), term))
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Users fetched successfully.",
                        users
                )
        );
    }

    @PatchMapping("/{userId}/status")
    @Transactional
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        UserStatus newStatus = request.status();

        if (newStatus != UserStatus.ACTIVE
                && newStatus != UserStatus.BLOCKED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only ACTIVE or BLOCKED status is allowed."
            );
        }

        if (user.getRole() == UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Admin accounts cannot be managed from this screen."
            );
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.getName() != null
                && authentication.getName().equalsIgnoreCase(user.getEmail())
                && newStatus == UserStatus.BLOCKED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot block your own account."
            );
        }

        user.setStatus(newStatus);
        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "User status updated successfully.",
                        toResponse(savedUser)
                )
        );
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getEmailVerified(),
                user.getPhoneVerified(),
                user.getCreatedAt()
        );
    }

    private boolean contains(String value, String term) {
        return value != null
                && value.toLowerCase(Locale.ROOT).contains(term);
    }
}