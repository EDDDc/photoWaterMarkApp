package com.photowatermarkapp.service;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class FontService {

    public List<String> listSystemFonts() {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fonts = environment.getAvailableFontFamilyNames();
        return Arrays.stream(fonts)
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
    }
}