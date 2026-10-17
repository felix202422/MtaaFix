package com.mtaafix.config.controller;

import com.mtaafix.config.dto.SettingsDto;
import com.mtaafix.config.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Settings", description = "System settings (P24; read-only in MVP per FR-12.3)")
@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Operation(summary = "Effective system settings (ADMINISTRATOR only; secrets masked)")
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<SettingsDto> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }
}
