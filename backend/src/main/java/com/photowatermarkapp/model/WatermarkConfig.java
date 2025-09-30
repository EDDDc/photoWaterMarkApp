package com.photowatermarkapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WatermarkConfig {

    private String type; // text | image
    private TextWatermarkConfig text;
    private ImageWatermarkConfig image;
    private LayoutConfig layout;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public TextWatermarkConfig getText() {
        return text;
    }

    public void setText(TextWatermarkConfig text) {
        this.text = text;
    }

    public ImageWatermarkConfig getImage() {
        return image;
    }

    public void setImage(ImageWatermarkConfig image) {
        this.image = image;
    }

    public LayoutConfig getLayout() {
        return layout;
    }

    public void setLayout(LayoutConfig layout) {
        this.layout = layout;
    }
}