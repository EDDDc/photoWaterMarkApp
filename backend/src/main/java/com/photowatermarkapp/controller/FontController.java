package com.photowatermarkapp.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photowatermarkapp.service.FontService;

@RestController
@RequestMapping("/api/fonts")
public class FontController {

    private final FontService fontService;

    public FontController(FontService fontService) {
        this.fontService = fontService;
    }

    @GetMapping
    public List<String> listFonts() {
        return fontService.listSystemFonts();
    }
}