package com.photowatermarkapp.model.export;

import com.photowatermarkapp.model.ExportConfig;
import com.photowatermarkapp.model.WatermarkConfig;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class ExportRequest {

    @NotNull
    @Valid
    private WatermarkConfig watermarkConfig;

    @NotNull
    @Valid
    private ExportConfig exportConfig;

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
