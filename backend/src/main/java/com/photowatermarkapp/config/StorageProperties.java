package com.photowatermarkapp.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private static final Logger log = LoggerFactory.getLogger(StorageProperties.class);

    private String baseDir;
    private Path resolvedBaseDir;

    public String getBaseDir() {
        return ensureBaseDir().toString();
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
        this.resolvedBaseDir = null;
    }

    public Path resolve(String first, String... more) {
        Path root = ensureBaseDir();
        return more == null || more.length == 0 ? root.resolve(first) : root.resolve(Paths.get(first, more));
    }

    private synchronized Path ensureBaseDir() {
        if (resolvedBaseDir != null) {
            return resolvedBaseDir;
        }

        Path defaultDir = defaultBaseDirPath();

        if (baseDir != null && !baseDir.isBlank()) {
            Path candidate = resolveConfiguredBaseDir(baseDir);
            Path usable = createDirectory(candidate);
            if (usable != null) {
                resolvedBaseDir = usable;
                return usable;
            }

            Path fallback = createDirectory(defaultDir);
            if (fallback != null) {
                log.warn("无法使用配置的存储目录 {}，已回退至 {}", candidate, fallback);
                resolvedBaseDir = fallback;
                baseDir = fallback.toString();
                return fallback;
            }

            throw new IllegalStateException("无法创建存储目录: " + candidate);
        }

        Path fallback = createDirectory(defaultDir);
        if (fallback == null) {
            throw new IllegalStateException("无法创建存储目录: " + defaultDir);
        }
        resolvedBaseDir = fallback;
        baseDir = fallback.toString();
        return fallback;
    }

    private Path createDirectory(Path dir) {
        try {
            Files.createDirectories(dir);
            return dir;
        } catch (IOException ex) {
            log.warn("创建存储目录 {} 失败", dir, ex);
            return null;
        }
    }

    private Path defaultBaseDirPath() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Paths.get(appData, "PhotoWatermark").toAbsolutePath().normalize();
        }
        String userHome = System.getProperty("user.home", ".");
        return Paths.get(userHome, ".photo-watermark").toAbsolutePath().normalize();
    }

    private Path resolveConfiguredBaseDir(String value) {
        Path configured = Paths.get(value);
        if (!configured.isAbsolute()) {
            configured = Paths.get("").toAbsolutePath().resolve(configured);
        }
        return configured.normalize();
    }
}
