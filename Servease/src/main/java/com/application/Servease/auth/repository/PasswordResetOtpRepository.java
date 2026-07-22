package com.application.Servease.auth.repository;

import com.application.Servease.auth.entity.PasswordResetOtp;
import com.application.Servease.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findByUser(User user);

    boolean existsByUser(User user);

    void deleteByUser(User user);
}