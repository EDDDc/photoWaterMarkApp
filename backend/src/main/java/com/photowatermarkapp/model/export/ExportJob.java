package com.photowatermarkapp.model.export;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExportJob {

    private final String id;
    private final Instant createdAt;
    private volatile Instant updatedAt;
    private volatile ExportJobStatus status;
    private volatile String message;
    private volatile String currentFile;
    private volatile String outputDirectory;

    private final AtomicInteger totalFiles = new AtomicInteger();
    private final AtomicInteger processedFiles = new AtomicInteger();
    private final AtomicInteger successCount = new AtomicInteger();
    private final AtomicInteger failureCount = new AtomicInteger();

    private final List<ExportFileResult> results = new CopyOnWriteArrayList<>();

    @JsonIgnore
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    public ExportJob(String id) {
        this.id = id;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.status = ExportJobStatus.QUEUED;
    }

    public String getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ExportJobStatus getStatus() {
        return status;
    }

    public void setStatus(ExportJobStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
        this.updatedAt = Instant.now();
    }

    public String getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(String currentFile) {
        this.currentFile = currentFile;
        this.updatedAt = Instant.now();
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public int getTotalFiles() {
        return totalFiles.get();
    }

    public void setTotalFiles(int total) {
        this.totalFiles.set(total);
    }

    public int getProcessedFiles() {
        return processedFiles.get();
    }

    public int incrementProcessed() {
        return processedFiles.incrementAndGet();
    }

    public int getSuccessCount() {
        return successCount.get();
    }

    public void incrementSuccess() {
        successCount.incrementAndGet();
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    public void incrementFailure() {
        failureCount.incrementAndGet();
    }

    public List<ExportFileResult> getResults() {
        return results;
    }

    public void addResult(ExportFileResult result) {
        results.add(result);
        this.updatedAt = Instant.now();
    }

    public boolean isCancelRequested() {
        return cancelRequested.get();
    }

    public void requestCancel() {
        cancelRequested.set(true);
        this.updatedAt = Instant.now();
    }
}
