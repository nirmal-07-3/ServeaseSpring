package com.application.Servease.auth.service;


import com.application.Servease.auth.dto.*;


import com.application.Servease.auth.entity.PasswordResetOtp;
import com.application.Servease.auth.repository.PasswordResetOtpRepository;
import com.application.Servease.common.exception.*;
import com.application.Servease.common.service.EmailService;
import com.application.Servease.entity.User;
import com.application.Servease.entity.enums.UserRole;
import com.application.Servease.entity.enums.UserStatus;
import com.application.Servease.repository.UserRepository;
import com.application.Servease.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;






@Service
public class AuthService {


    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordResetOtpRepository passwordResetOtpRepository;

    private final JwtService jwtService;

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int OTP_RESEND_MINUTES = 1;


    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder, EmailService emailService, PasswordResetOtpRepository passwordResetOtpRepository,
                       JwtService jwtService) {


        this.userRepository = userRepository;
        this.passwordEncoder = (BCryptPasswordEncoder) passwordEncoder;
        this.emailService = emailService;
        this.passwordResetOtpRepository = passwordResetOtpRepository;
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




    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        // Step 1: Find User
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found."));

        // Step 2: Check if OTP already exists
        Optional<PasswordResetOtp> existingOtp =
                passwordResetOtpRepository.findByUser(user);

        if (existingOtp.isPresent()) {

            PasswordResetOtp otpEntity = existingOtp.get();

            // Step 3: Prevent OTP spam (1 minute)
            if (otpEntity.getCreatedAt()
                    .plusMinutes(1)
                    .isAfter(LocalDateTime.now())) {

                throw new OtpRequestTooFrequentException(
                        "Please wait 1 minute before requesting another OTP.");
            }

            // Step 4: Delete old OTP
            passwordResetOtpRepository.delete(otpEntity);
            passwordResetOtpRepository.flush();
        }

        // Step 5: Generate 6-digit OTP
        SecureRandom random = new SecureRandom();
        String otp = String.valueOf(100000 + random.nextInt(900000));

        // Step 6: Create OTP entity
        PasswordResetOtp passwordResetOtp = new PasswordResetOtp();

        passwordResetOtp.setUser(user);
        passwordResetOtp.setOtpHash(passwordEncoder.encode(otp));
        passwordResetOtp.setCreatedAt(LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();

        passwordResetOtp.setCreatedAt(now);
        passwordResetOtp.setExpiryTime(now.plusMinutes(OTP_EXPIRY_MINUTES));
        passwordResetOtp.setVerified(false);

        // Step 7: Save OTP
        passwordResetOtpRepository.save(passwordResetOtp);

        // Step 8: Send Email
        emailService.sendOtpEmail(user.getEmail(), otp);
    }

    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {

        // Step 1: Find User
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found."));

        // Step 2: Find OTP
        PasswordResetOtp passwordResetOtp = passwordResetOtpRepository
                .findByUser(user)
                .orElseThrow(() ->
                        new InvalidOtpException("OTP not found."));

        // Step 3: Check Expiry
        if (passwordResetOtp.getExpiryTime().isBefore(LocalDateTime.now())) {

            passwordResetOtpRepository.delete(passwordResetOtp);

            throw new OtpExpiredException("OTP has expired.");
        }

        // Step 4: Verify OTP
        if (!passwordEncoder.matches(request.getOtp(),
                passwordResetOtp.getOtpHash())) {

            throw new InvalidOtpException("Invalid OTP.");
        }

        // Step 5: Mark as Verified
        passwordResetOtp.setVerified(true);

        passwordResetOtpRepository.save(passwordResetOtp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        // Step 1: Check if user exists
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found."));

        // Step 2: Find OTP
        PasswordResetOtp passwordResetOtp = passwordResetOtpRepository
                .findByUser(user)
                .orElseThrow(() ->
                        new InvalidOtpException("OTP verification required."));

        // Step 3: Check OTP verification
        if (!passwordResetOtp.isVerified()) {
            throw new InvalidOtpException("Please verify your OTP first.");
        }

        // Step 4: Check password confirmation
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new PasswordMismatchException("Passwords do not match.");
        }

        // Step 5: Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);

        // Step 6: Delete OTP
        passwordResetOtpRepository.delete(passwordResetOtp);
        passwordResetOtpRepository.flush();
    }


    }



