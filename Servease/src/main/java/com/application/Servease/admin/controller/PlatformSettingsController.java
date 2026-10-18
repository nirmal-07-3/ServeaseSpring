package com.application.Servease.admin.controller;

import com.application.Servease.admin.dto.PlatformSettingsRequest;
import com.application.Servease.admin.dto.PlatformSettingsResponse;
import com.application.Servease.admin.service.PlatformSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class PlatformSettingsController {

    private final PlatformSettingsService settingsService;

    @GetMapping
    public PlatformSettingsResponse getSettings() {
        return settingsService.getSettings();
    }

    @PutMapping
    public PlatformSettingsResponse updateSettings(
            @RequestBody PlatformSettingsRequest request) {
        return settingsService.updateSettings(request);
    }
}