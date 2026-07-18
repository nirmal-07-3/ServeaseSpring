package com.application.Servease.auth.controller;

import com.application.Servease.auth.dto.AuthResponse;
import com.application.Servease.auth.dto.LoginRequest;
import com.application.Servease.auth.dto.RegisterRequest;
import com.application.Servease.auth.service.AuthService;
import com.application.Servease.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @RequestBody RegisterRequest request){

        authService.register(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "User registered successfully",
                        null
                )
        );
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Login successful",
                        response
                )
        );
    }


    @RestController
    @RequestMapping("/api/test")
    public class TestController {

        @GetMapping("/hello")
        public String hello() {
            return "JWT Authentication Working!";
        }
    }
}