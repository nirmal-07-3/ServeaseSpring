
package com.application.Servease.favorite.controller;

import com.application.Servease.favorite.entity.FavoriteProvider;
import com.application.Servease.favorite.service.FavoriteProviderService;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/favorites")
@RequiredArgsConstructor
public class FavoriteProviderController {

    private final FavoriteProviderService favoriteService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<FavoriteResponse>> getFavorites(
            Authentication authentication) {

        Long customerId = getAuthenticatedUserId(authentication);

        List<FavoriteResponse> favorites = favoriteService
                .getFavorites(customerId)
                .stream()
                .map(FavoriteResponse::from)
                .toList();

        return ResponseEntity.ok(favorites);
    }

    @PostMapping("/{providerId}")
    public ResponseEntity<FavoriteResponse> addFavorite(
            @PathVariable Long providerId,
            Authentication authentication) {

        Long customerId = getAuthenticatedUserId(authentication);

        FavoriteProvider favorite =
                favoriteService.addFavorite(customerId, providerId);

        return ResponseEntity.ok(FavoriteResponse.from(favorite));
    }

    @DeleteMapping("/{providerId}")
    public ResponseEntity<Map<String, String>> removeFavorite(
            @PathVariable Long providerId,
            Authentication authentication) {

        Long customerId = getAuthenticatedUserId(authentication);

        favoriteService.removeFavorite(customerId, providerId);

        return ResponseEntity.ok(
                Map.of("message", "Provider removed from favorites")
        );
    }

    private Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Authentication is required");
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new EntityNotFoundException("Authenticated user not found"));

        return user.getId();
    }

    public record FavoriteResponse(
            Long id,
            Long providerId,
            String businessName,
            String description,
            String address,
            String city,
            Double averageRating,
            Integer totalReviews
    ) {
        static FavoriteResponse from(FavoriteProvider favorite) {
            var provider = favorite.getProvider();

            return new FavoriteResponse(
                    favorite.getId(),
                    provider.getId(),
                    provider.getBusinessName(),
                    provider.getDescription(),
                    provider.getAddress(),
                    provider.getCity(),
                    provider.getAverageRating(),
                    provider.getTotalReviews()
            );
        }
    }
}
