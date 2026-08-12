package com.application.Servease.common.exception;

import com.application.Servease.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===========================
    // Validation Exceptions
    // ===========================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(
                new ApiResponse<>(
                        false,
                        "Validation failed.",
                        errors
                )
        );
    }

    // ===========================
    // Unauthorized (401)
    // ===========================
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidCredentials(
            InvalidCredentialsException ex) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ApiResponse<>(
                        false,
                        ex.getMessage(),
                        null
                )
        );
    }

    // ===========================
    // Conflict (409)
    // ===========================
    @ExceptionHandler({
            DuplicateEmailException.class,
            DuplicatePhoneException.class,
            DuplicateDocumentException.class,
            CategoryAlreadyExistsException.class,
            SubCategoryAlreadyExistsException.class,
            ProviderAlreadyExistsException.class,
            ProviderServiceAlreadyExistsException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleConflict(
            RuntimeException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiResponse<>(
                        false,
                        ex.getMessage(),
                        null
                )
        );
    }

    // ===========================
    // Not Found (404)
    // ===========================
    @ExceptionHandler({
            UserNotFoundException.class,
            ProviderNotFoundException.class,
            CategoryNotFoundException.class,
            SubCategoryNotFoundException.class,
            ProviderServiceNotFoundException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleNotFound(
            RuntimeException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiResponse<>(
                        false,
                        ex.getMessage(),
                        null
                )
        );
    }

    // ===========================
    // Bad Request (400)
    // ===========================
    @ExceptionHandler({
            InvalidOtpException.class,
            OtpExpiredException.class,
            OtpRequestTooFrequentException.class,
            PasswordMismatchException.class,
            InvalidAvailabilityException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(
            RuntimeException ex) {

        return ResponseEntity.badRequest().body(
                new ApiResponse<>(
                        false,
                        ex.getMessage(),
                        null
                )
        );
    }

    // ===========================
    // Internal Server Error (500)
    // ===========================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(
            Exception ex) {

        ex.printStackTrace();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(
                        false,
                        "Something went wrong. Please try again later.",
                        null
                )
        );
    }

    @ExceptionHandler(ProviderNotApprovedException.class)
    public ResponseEntity<ApiResponse<Object>> handleProviderNotApproved(
            ProviderNotApprovedException ex) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(
                        false,
                        ex.getMessage(),
                        null
                ));
    }
}