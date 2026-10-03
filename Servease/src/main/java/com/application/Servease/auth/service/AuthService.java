package com.application.Servease.auth.service;


import com.application.Servease.auth.dto.*;


import com.application.Servease.auth.entity.PasswordResetOtp;
import com.application.Servease.auth.repository.PasswordResetOtpRepository;
import com.application.Servease.common.exception.*;
import com.application.Servease.common.service.EmailService;
import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.provider.enums.VerificationStatus;
import com.application.Servease.provider.repository.ProviderProfileRepository;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.enums.UserRole;
import com.application.Servease.user.enums.UserStatus;
import com.application.Servease.user.repository.UserRepository;
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
    private final ProviderProfileRepository providerProfileRepository;

    private final JwtService jwtService;

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int OTP_RESEND_MINUTES = 1;


    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            PasswordResetOtpRepository passwordResetOtpRepository,
            JwtService jwtService,
            ProviderProfileRepository providerProfileRepository) {

        this.userRepository = userRepository;
        this.passwordEncoder = (BCryptPasswordEncoder) passwordEncoder;
        this.emailService = emailService;
        this.passwordResetOtpRepository = passwordResetOtpRepository;
        this.jwtService = jwtService;
        this.providerProfileRepository = providerProfileRepository;
    }

    @Transactional
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


        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword()) //Encrypt
        );


        user.setRole(request.getRole());


        if (request.getRole() == UserRole.PROVIDER) {
            user.setStatus(UserStatus.PENDING);
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }


        user.setEmailVerified(false);
        user.setPhoneVerified(false);


        User savedUser = userRepository.save(user);


        if (request.getRole() == UserRole.PROVIDER) {

            ProviderProfile providerProfile =
                    ProviderProfile.builder()
                            .user(savedUser)
                            .businessName(savedUser.getName())
                            .profileCompleted(false)
                            .verificationStatus(
                                    VerificationStatus.PENDING
                            )
                            .averageRating(0.0)
                            .totalReviews(0)
                            .totalBookings(0)
                            .build();

            providerProfileRepository.save(providerProfile);
        }
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


        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );


        PasswordResetOtp passwordResetOtp =
                passwordResetOtpRepository
                        .findByUser(user)
                        .orElse(null);


        if (passwordResetOtp != null) {

            if (passwordResetOtp.getCreatedAt()
                    .plusMinutes(1)
                    .isAfter(LocalDateTime.now())) {

                throw new OtpRequestTooFrequentException(
                        "Please wait 1 minute before requesting another OTP."
                );
            }

        } else {


            passwordResetOtp = new PasswordResetOtp();

            passwordResetOtp.setUser(user);
        }


        SecureRandom secureRandom = new SecureRandom();

        String otp = String.valueOf(
                100000 + secureRandom.nextInt(900000)
        );


        passwordResetOtp.setOtpHash(
                passwordEncoder.encode(otp)
        );

        passwordResetOtp.setCreatedAt(
                LocalDateTime.now()
        );

        passwordResetOtp.setExpiryTime(
                LocalDateTime.now().plusMinutes(5)
        );

        passwordResetOtp.setVerified(false);


        passwordResetOtpRepository.saveAndFlush(
                passwordResetOtp
        );


        emailService.sendOtpEmail(
                user.getEmail(),
                otp
        );
    }

    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {


        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found."));


        PasswordResetOtp passwordResetOtp = passwordResetOtpRepository
                .findByUser(user)
                .orElseThrow(() ->
                        new InvalidOtpException("OTP not found."));


        if (passwordResetOtp.getExpiryTime().isBefore(LocalDateTime.now())) {

            passwordResetOtpRepository.delete(passwordResetOtp);

            throw new OtpExpiredException("OTP has expired.");
        }


        if (!passwordEncoder.matches(request.getOtp(),
                passwordResetOtp.getOtpHash())) {

            throw new InvalidOtpException("Invalid OTP.");
        }


        passwordResetOtp.setVerified(true);

        passwordResetOtpRepository.save(passwordResetOtp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {


        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found."));


        PasswordResetOtp passwordResetOtp = passwordResetOtpRepository
                .findByUser(user)
                .orElseThrow(() ->
                        new InvalidOtpException("OTP verification required."));


        if (!passwordResetOtp.isVerified()) {
            throw new InvalidOtpException("Please verify your OTP first.");
        }


        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new PasswordMismatchException("Passwords do not match.");
        }


        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);


        passwordResetOtpRepository.delete(passwordResetOtp);
        passwordResetOtpRepository.flush();
    }


    }



