package com.photowatermarkapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExportConfig {

    private String outputDir;
    private String format;
    private Integer jpegQuality;
    private ResizeConfig resize;
    private NamingRule naming;

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Integer getJpegQuality() {
        return jpegQuality;
    }

    public void setJpegQuality(Integer jpegQuality) {
        this.jpegQuality = jpegQuality;
    }

    public ResizeConfig getResize() {
        return resize;
    }

    public void setResize(ResizeConfig resize) {
        this.resize = resize;
    }

    public NamingRule getNaming() {
        return naming;
    }

    public void setNaming(NamingRule naming) {
        this.naming = naming;
    }
}