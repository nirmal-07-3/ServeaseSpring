package com.application.Servease.favorite.repository;

import com.application.Servease.favorite.entity.FavoriteProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteProviderRepository extends JpaRepository<FavoriteProvider, Long> {

    List<FavoriteProvider> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    Optional<FavoriteProvider> findByCustomer_IdAndProvider_Id(
            Long customerId,
            Long providerId
    );

    boolean existsByCustomer_IdAndProvider_Id(Long customerId, Long providerId);

    void deleteByCustomer_IdAndProvider_Id(Long customerId, Long providerId);
}
