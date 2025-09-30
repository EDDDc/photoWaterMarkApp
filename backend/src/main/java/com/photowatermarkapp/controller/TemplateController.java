package com.photowatermarkapp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photowatermarkapp.model.Template;
import com.photowatermarkapp.model.TemplateRequest;
import com.photowatermarkapp.service.TemplateService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/templates")
@Validated
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public List<Template> listTemplates() {
        return templateService.listTemplates();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Template> getTemplate(@PathVariable String id) {
        return templateService.findTemplate(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Template> saveTemplate(@Valid @RequestBody TemplateRequest request) {
        Template saved = templateService.saveTemplate(request);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}