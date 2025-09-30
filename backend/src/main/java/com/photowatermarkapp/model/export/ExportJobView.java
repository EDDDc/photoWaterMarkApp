package com.photowatermarkapp.model.export;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExportJobView {

    private String id;
    private ExportJobStatus status;
    private String message;
    private String currentFile;
    private String outputDirectory;
    private int totalFiles;
    private int processedFiles;
    private int successCount;
    private int failureCount;
    private double progress;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ExportFileResult> results;

    public static ExportJobView from(ExportJob job) {
        ExportJobView view = new ExportJobView();
        view.id = job.getId();
        view.status = job.getStatus();
        view.message = job.getMessage();
        view.currentFile = job.getCurrentFile();
        view.outputDirectory = job.getOutputDirectory();
        view.totalFiles = job.getTotalFiles();
        view.processedFiles = job.getProcessedFiles();
        view.successCount = job.getSuccessCount();
        view.failureCount = job.getFailureCount();
        view.progress = view.totalFiles == 0 ? 0d : (double) view.processedFiles / view.totalFiles;
        view.createdAt = job.getCreatedAt();
        view.updatedAt = job.getUpdatedAt();
        view.results = job.getResults();
        return view;
    }

    public String getId() {
        return id;
    }

    public ExportJobStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getCurrentFile() {
        return currentFile;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public int getProcessedFiles() {
        return processedFiles;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public double getProgress() {
        return progress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ExportFileResult> getResults() {
        return results;
    }
}
