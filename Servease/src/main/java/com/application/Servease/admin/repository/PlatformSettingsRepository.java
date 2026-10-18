package com.application.Servease.admin.repository;

import com.application.Servease.admin.entity.PlatformSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformSettingsRepository
        extends JpaRepository<PlatformSettings, Long> {
}