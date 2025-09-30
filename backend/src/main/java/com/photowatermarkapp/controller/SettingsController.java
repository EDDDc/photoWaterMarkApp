package com.photowatermarkapp.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photowatermarkapp.model.LastSettings;
import com.photowatermarkapp.model.LastSettingsRequest;
import com.photowatermarkapp.service.SettingsService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/settings")
@Validated
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/last")
    public ResponseEntity<LastSettings> getLastSettings() {
        Optional<LastSettings> settings = settingsService.readLastSettings();
        return settings.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/last")
    public ResponseEntity<LastSettings> saveLastSettings(@Valid @RequestBody LastSettingsRequest request) {
        LastSettings saved = settingsService.saveLastSettings(request);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/last")
    public ResponseEntity<Void> deleteLastSettings() {
        settingsService.deleteLastSettings();
        return ResponseEntity.noContent().build();
    }
}