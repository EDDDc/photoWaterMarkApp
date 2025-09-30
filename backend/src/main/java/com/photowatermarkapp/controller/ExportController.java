package com.photowatermarkapp.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.photowatermarkapp.model.export.ExportJob;
import com.photowatermarkapp.model.export.ExportJobView;
import com.photowatermarkapp.model.export.ExportRequest;
import com.photowatermarkapp.service.ExportService;

@RestController
@RequestMapping("/api/export")
@Validated
public class ExportController {

    private final ExportService exportService;
    private final ObjectMapper objectMapper;

    public ExportController(ExportService exportService, ObjectMapper objectMapper) {
        this.exportService = exportService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExportJobView> submitExport(
            @RequestPart("config") String configJson,
            @RequestPart("files") List<MultipartFile> files) throws JsonProcessingException {
        ExportRequest request = objectMapper.readValue(configJson, ExportRequest.class);
        ExportJob job = exportService.submitExport(files, request);
        return ResponseEntity.accepted().body(ExportJobView.from(job));
    }

    @GetMapping("/{jobId}/status")
    public ResponseEntity<ExportJobView> getStatus(@PathVariable String jobId) {
        return exportService.findJob(jobId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<ExportJobView> cancelJob(@PathVariable String jobId) {
        boolean cancelled = exportService.cancelJob(jobId);
        if (!cancelled) {
            return ResponseEntity.notFound().build();
        }
        return exportService.findJob(jobId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<ExportJobView> listJobs() {
        return exportService.listJobs();
    }
}
