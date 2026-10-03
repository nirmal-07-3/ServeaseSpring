package com.application.Servease.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }


    // ============================================================
    // PASSWORD ENCODER
    // ============================================================

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }


    // ============================================================
    // CORS
    // ============================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:4200"
                )
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of("*")
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }


    // ============================================================
    // SECURITY FILTER CHAIN
    // ============================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // =================================================
                // CORS
                // =================================================

                .cors(cors -> cors
                        .configurationSource(
                                corsConfigurationSource()
                        )
                )


                // =================================================
                // CSRF
                // =================================================

                .csrf(csrf -> csrf.disable())


                // =================================================
                // STATELESS JWT SESSION
                // =================================================

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )


                // =================================================
                // AUTHORIZATION
                // =================================================

                .authorizeHttpRequests(auth -> auth


                        // =========================================
                        // PUBLIC APIs
                        // =========================================

                        .requestMatchers(
                                "/api/auth/**",
                                "/api/categories/**",
                                "/api/subcategories/**",
                                "/api/payments/razorpay/webhook"
                        )
                        .permitAll()


                        // =========================================
                        // SWAGGER
                        // =========================================

                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        )
                        .permitAll()


                        // =========================================
                        // PUBLIC HTML PAGES
                        // =========================================

                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/login.html",
                                "/register.html",
                                "/forgot-password.html",
                                "/verify-otp.html",
                                "/reset-password.html",
                                "/razorpay-test.html"
                        )
                        .permitAll()


                        // =========================================
                        // STATIC RESOURCES
                        // =========================================

                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/assets/**",
                                "/fonts/**",
                                "/favicon.ico"
                        )
                        .permitAll()


                        // =========================================
                        // ADMIN
                        // =========================================

                        .requestMatchers(
                                "/api/admin/**"
                        )
                        .hasRole("ADMIN")


                        // =========================================
                        // PROVIDER
                        // =========================================

                        .requestMatchers(
                                "/api/providers/**"
                        )
                        .hasAnyRole(
                                "PROVIDER",
                                "ADMIN"
                        )


                        // =========================================
                        // USERS
                        // =========================================

                        .requestMatchers(
                                "/api/users/**"
                        )
                        .hasAnyRole(
                                "CUSTOMER",
                                "PROVIDER",
                                "ADMIN"
                        )


                        // =========================================
                        // EVERYTHING ELSE
                        // =========================================

                        .anyRequest()
                        .authenticated()
                )


                // =================================================
                // JWT FILTER
                // =================================================

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );


        return http.build();
    }
}