package com.application.Servease.provider.repository;

import com.application.Servease.provider.entity.ProviderAvailability;
import com.application.Servease.provider.entity.ProviderProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.DayOfWeek;

public interface ProviderAvailabilityRepository
        extends JpaRepository<ProviderAvailability, Long> {

    List<ProviderAvailability> findByProvider(ProviderProfile provider);

    Optional<ProviderAvailability> findByProviderAndDayOfWeek(
            ProviderProfile provider,
            DayOfWeek dayOfWeek
    );

    void deleteByProvider(ProviderProfile provider);
}