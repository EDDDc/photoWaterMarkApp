package com.photowatermarkapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemplateRequest {

    private String id;

    @NotBlank(message = "模板名称不能为空")
    private String name;

    @NotNull(message = "水印配置不能为空")
    @Valid
    private WatermarkConfig watermarkConfig;

    @NotNull(message = "导出配置不能为空")
    @Valid
    private ExportConfig exportConfig;

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
}