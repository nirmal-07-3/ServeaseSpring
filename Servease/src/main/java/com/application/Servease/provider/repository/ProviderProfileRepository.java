package com.application.Servease.provider.repository;

import com.application.Servease.provider.entity.ProviderProfile;
import com.application.Servease.provider.enums.VerificationStatus;
import com.application.Servease.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderProfileRepository extends JpaRepository<ProviderProfile, Long> {

    Optional<ProviderProfile> findByUser(User user);

    boolean existsByUser(User user);

    List<ProviderProfile> findByVerificationStatus(
            VerificationStatus verificationStatus
    );

    long countByVerificationStatus(VerificationStatus verificationStatus);
}