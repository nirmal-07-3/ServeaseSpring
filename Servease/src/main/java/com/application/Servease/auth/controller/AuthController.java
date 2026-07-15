package com.application.Servease.auth.controller;

import com.application.Servease.auth.dto.RegisterRequest;
import com.application.Servease.auth.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


    @RestController
    @RequestMapping("/api/auth")
    public class AuthController {

        private final AuthService authService;

        public AuthController(AuthService authService) {
            this.authService = authService;
        }

        @PostMapping("/register")
        public String register(@RequestBody RegisterRequest request) {
            return authService.register(request);
        }
    }

