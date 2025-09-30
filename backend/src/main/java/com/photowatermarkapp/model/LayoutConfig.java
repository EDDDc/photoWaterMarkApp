package com.photowatermarkapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LayoutConfig {

    private String preset;
    private Double x;
    private Double y;
    private Double rotationDeg;
    private Double scale;

    public String getPreset() {
        return preset;
    }

    public void setPreset(String preset) {
        this.preset = preset;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getRotationDeg() {
        return rotationDeg;
    }

    public void setRotationDeg(Double rotationDeg) {
        this.rotationDeg = rotationDeg;
    }

    public Double getScale() {
        return scale;
    }

    public void setScale(Double scale) {
        this.scale = scale;
    }
}