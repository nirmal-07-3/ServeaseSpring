
package com.application.Servease.payment.controller;

import com.application.Servease.common.dto.ApiResponse;
import com.application.Servease.payment.dto.BookingOnlineQrResponse;
import com.application.Servease.payment.dto.DevelopmentCollectionSimulationRequest;
import com.application.Servease.payment.service.DevelopmentCollectionSimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/provider/bookings")
@RequiredArgsConstructor
@Profile("dev")
@ConditionalOnProperty(
        name = "servease.payment.simulator.enabled",
        havingValue = "true"
)
public class DevelopmentCollectionSimulationController {

    private final DevelopmentCollectionSimulationService simulationService;

    @PostMapping("/{bookingId}/collection/dev-simulator")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<BookingOnlineQrResponse>> createTestCollection(
            @PathVariable Long bookingId) {

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "TEST ONLY: Development payment collection created. No real payment was requested.",
                simulationService.createPendingCollection(bookingId)
        ));
    }

    @PatchMapping("/{bookingId}/collection/dev-simulator")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<BookingOnlineQrResponse>> completeTestCollection(
            @PathVariable Long bookingId,
            @Valid @RequestBody DevelopmentCollectionSimulationRequest request) {

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "TEST ONLY: Development payment outcome recorded. No real payment occurred.",
                simulationService.completeSimulation(bookingId, request)
        ));
    }
}
