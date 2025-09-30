package com.photowatermarkapp.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /**
     * 基础存储目录，默认位于 Windows 的 %APPDATA%/PhotoWatermark。
     */
    private String baseDir;

    public String getBaseDir() {
        if (baseDir == null || baseDir.isBlank()) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Paths.get(appData, "PhotoWatermark").toString();
            }
            String userHome = System.getProperty("user.home", ".");
            return Paths.get(userHome, ".photo-watermark").toString();
        }
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public Path resolve(String first, String... more) {
        Path root = Paths.get(getBaseDir());
        if (!Files.exists(root)) {
            try {
                Files.createDirectories(root);
            } catch (Exception ex) {
                throw new IllegalStateException("无法创建存储目录: " + root, ex);
            }
        }
        return more == null || more.length == 0 ? root.resolve(first) : root.resolve(Paths.get(first, more));
    }
}