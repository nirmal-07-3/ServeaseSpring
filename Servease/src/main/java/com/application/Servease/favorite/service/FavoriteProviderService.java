
package com.application.Servease.favorite.service;

import com.application.Servease.favorite.entity.FavoriteProvider;
import com.application.Servease.favorite.repository.FavoriteProviderRepository;
import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.provider.enums.VerificationStatus;
import com.application.Servease.provider.repository.ProviderProfileRepository;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.enums.UserRole;
import com.application.Servease.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteProviderService {

    private final FavoriteProviderRepository favoriteRepository;
    private final ProviderProfileRepository providerRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<FavoriteProvider> getFavorites(Long customerId) {
        requireCustomer(customerId);
        return favoriteRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId);
    }

    @Transactional
    public FavoriteProvider addFavorite(Long customerId, Long providerId) {
        User customer = requireCustomer(customerId);

        ProviderProfile provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Provider not found"));

        if (provider.getUser().getId().equals(customerId)) {
            throw new IllegalArgumentException("You cannot favorite your own provider profile");
        }

        if (provider.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new IllegalArgumentException("Only approved providers can be favorited");
        }

        return favoriteRepository
                .findByCustomer_IdAndProvider_Id(customerId, providerId)
                .orElseGet(() -> {
                    FavoriteProvider favorite = new FavoriteProvider();
                    favorite.setCustomer(customer);
                    favorite.setProvider(provider);
                    return favoriteRepository.save(favorite);
                });
    }

    @Transactional
    public void removeFavorite(Long customerId, Long providerId) {
        requireCustomer(customerId);

        if (!providerRepository.existsById(providerId)) {
            throw new EntityNotFoundException("Provider not found");
        }

        favoriteRepository.deleteByCustomer_IdAndProvider_Id(customerId, providerId);
    }

    private User requireCustomer(Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new IllegalArgumentException("Only customers can manage favorite providers");
        }

        return customer;
    }
}
