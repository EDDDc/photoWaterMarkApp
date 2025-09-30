package com.photowatermarkapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TextWatermarkConfig {

    private String content;
    private String fontFamily;
    private Integer fontSize;
    private Boolean bold;
    private Boolean italic;
    private String color;
    private Double opacity;
    private StrokeStyle stroke;
    private ShadowStyle shadow;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    public Boolean getBold() {
        return bold;
    }

    public void setBold(Boolean bold) {
        this.bold = bold;
    }

    public Boolean getItalic() {
        return italic;
    }

    public void setItalic(Boolean italic) {
        this.italic = italic;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Double getOpacity() {
        return opacity;
    }

    public void setOpacity(Double opacity) {
        this.opacity = opacity;
    }

    public StrokeStyle getStroke() {
        return stroke;
    }

    public void setStroke(StrokeStyle stroke) {
        this.stroke = stroke;
    }

    public ShadowStyle getShadow() {
        return shadow;
    }

    public void setShadow(ShadowStyle shadow) {
        this.shadow = shadow;
    }
}