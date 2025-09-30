package com.photowatermarkapp.util;

import java.awt.Color;

public final class ColorUtils {

    private ColorUtils() {
    }

    public static Color parseColor(String value, Color fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String text = value.trim();
        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        try {
            if (text.length() == 6) {
                int rgb = Integer.parseInt(text, 16);
                return new Color(rgb);
            }
            if (text.length() == 8) {
                int argb = (int) Long.parseLong(text, 16);
                return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
            }
        } catch (NumberFormatException ex) {
            return fallback;
        }
        return fallback;
    }
}
