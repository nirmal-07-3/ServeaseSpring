package com.application.Servease.auth.service;


import com.application.Servease.auth.dto.AuthResponse;
import com.application.Servease.auth.dto.LoginRequest;
import com.application.Servease.auth.dto.RegisterRequest;


import com.application.Servease.common.exception.DuplicateEmailException;
import com.application.Servease.common.exception.DuplicatePhoneException;
import com.application.Servease.common.exception.InvalidCredentialsException;
import com.application.Servease.entity.User;
import com.application.Servease.entity.enums.UserRole;
import com.application.Servease.entity.enums.UserStatus;
import com.application.Servease.repository.UserRepository;
import com.application.Servease.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
public class AuthService {


    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private final JwtService jwtService;


    public AuthService( UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {


        this.userRepository = userRepository;
        this.passwordEncoder = (BCryptPasswordEncoder) passwordEncoder;
        this.jwtService = jwtService;
    }

    public void register(RegisterRequest request) {

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already registered");
        }

        // Check if phone already exists
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicatePhoneException("Phone number already registered");
        }

        // Create new user
        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        // Encrypt password
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        // Set role
        user.setRole(request.getRole());

        // Set status based on role
        if (request.getRole() == UserRole.PROVIDER) {
            user.setStatus(UserStatus.PENDING);
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }

        // Default values
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // Save user
        userRepository.save(user);
    }


    public AuthResponse login(LoginRequest loginrequest) {

        System.out.println("Login API called");

        if (loginrequest.getEmail() == null ||
                loginrequest.getPassword() == null ||
                loginrequest.getPassword().length() < 6) {

            System.out.println("Validation failed");
            throw new InvalidCredentialsException("Invalid Login Request");
        }

        User user = userRepository.findByEmail(loginrequest.getEmail()).orElse(null);

        if (user == null) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        System.out.println("User found: " + user.getEmail());

        if (!passwordEncoder.matches(
                loginrequest.getPassword(),
                user.getPasswordHash())) {

            throw new InvalidCredentialsException("Invalid email or password");
        }

        System.out.println("Password matched");

        AuthResponse response = new AuthResponse();

        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());

        response.setToken(
                jwtService.generateToken(
                        user.getEmail(),
                        user.getRole()
                )
        );

        System.out.println("Returning response");

        return response;
    }

    }



