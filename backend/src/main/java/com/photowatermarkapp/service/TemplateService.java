package com.photowatermarkapp.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.photowatermarkapp.config.StorageProperties;
import com.photowatermarkapp.model.Template;
import com.photowatermarkapp.model.TemplateRequest;

@Service
public class TemplateService {

    private static final String TEMPLATE_DIR_NAME = "templates";

    private final ObjectMapper objectMapper;
    private final StorageProperties storageProperties;

    public TemplateService(ObjectMapper objectMapper, StorageProperties storageProperties) {
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        this.storageProperties = storageProperties;
    }

    public List<Template> listTemplates() {
        Path dir = ensureTemplateDir();
        if (!Files.exists(dir)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(this::readTemplateSafely)
                    .flatMap(Optional::stream)
                    .sorted(Comparator.comparing(Template::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new IllegalStateException("读取模板列表失败", ex);
        }
    }

    public Template saveTemplate(TemplateRequest request) {
        Template template = new Template();
        String id = StringUtils.hasText(request.getId()) ? request.getId().trim() : UUID.randomUUID().toString();
        template.setId(id);
        template.setName(request.getName().trim());
        template.setWatermarkConfig(request.getWatermarkConfig());
        template.setExportConfig(request.getExportConfig());
        template.setUpdatedAt(Instant.now());

        Path file = templateFile(id);
        try {
            Files.createDirectories(file.getParent());
            objectMapper.writeValue(file.toFile(), template);
        } catch (IOException ex) {
            throw new IllegalStateException("保存模板失败: " + id, ex);
        }
        return template;
    }

    public Optional<Template> findTemplate(String id) {
        Path file = templateFile(id);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return readTemplateSafely(file);
    }

    public void deleteTemplate(String id) {
        Path file = templateFile(id);
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            throw new IllegalStateException("删除模板失败: " + id, ex);
        }
    }

    private Path ensureTemplateDir() {
        Path dir = storageProperties.resolve(TEMPLATE_DIR_NAME);
        try {
            Files.createDirectories(dir);
        } catch (IOException ex) {
            throw new IllegalStateException("无法创建模板目录", ex);
        }
        return dir;
    }

    private Path templateFile(String id) {
        return ensureTemplateDir().resolve(id + ".json");
    }

    private Optional<Template> readTemplateSafely(Path path) {
        try {
            return Optional.ofNullable(objectMapper.readValue(path.toFile(), Template.class));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }
}