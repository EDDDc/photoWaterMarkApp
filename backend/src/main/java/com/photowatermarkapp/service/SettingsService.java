package com.photowatermarkapp.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.photowatermarkapp.config.StorageProperties;
import com.photowatermarkapp.model.LastSettings;
import com.photowatermarkapp.model.LastSettingsRequest;

@Service
public class SettingsService {

    private static final String SETTINGS_DIR = "settings";
    private static final String LAST_SETTINGS_FILE = "last.json";

    private final ObjectMapper objectMapper;
    private final StorageProperties storageProperties;

    public SettingsService(ObjectMapper objectMapper, StorageProperties storageProperties) {
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        this.storageProperties = storageProperties;
    }

    public Optional<LastSettings> readLastSettings() {
        Path path = settingsFile();
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(objectMapper.readValue(path.toFile(), LastSettings.class));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    public LastSettings saveLastSettings(LastSettingsRequest request) {
        LastSettings settings = new LastSettings();
        settings.setWatermarkConfig(request.getWatermarkConfig());
        settings.setExportConfig(request.getExportConfig());
        settings.setUpdatedAt(Instant.now());

        Path path = settingsFile();
        try {
            Files.createDirectories(path.getParent());
            objectMapper.writeValue(path.toFile(), settings);
        } catch (IOException ex) {
            throw new IllegalStateException("保存最近设置失败", ex);
        }
        return settings;
    }

    public void deleteLastSettings() {
        Path path = settingsFile();
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new IllegalStateException("删除最近设置失败", ex);
        }
    }

    private Path settingsFile() {
        return storageProperties.resolve(SETTINGS_DIR, LAST_SETTINGS_FILE);
    }
}