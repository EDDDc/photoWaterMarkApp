package com.photowatermarkapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NamingRule {

    private Boolean keepOriginal;
    private String prefix;
    private String suffix;

    public Boolean getKeepOriginal() {
        return keepOriginal;
    }

    public void setKeepOriginal(Boolean keepOriginal) {
        this.keepOriginal = keepOriginal;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}