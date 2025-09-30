package com.photowatermarkapp.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Template {

    private String id;
    private String name;
    private WatermarkConfig watermarkConfig;
    private ExportConfig exportConfig;
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WatermarkConfig getWatermarkConfig() {
        return watermarkConfig;
    }

    public void setWatermarkConfig(WatermarkConfig watermarkConfig) {
        this.watermarkConfig = watermarkConfig;
    }

    public ExportConfig getExportConfig() {
        return exportConfig;
    }

    public void setExportConfig(ExportConfig exportConfig) {
        this.exportConfig = exportConfig;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}