package com.photowatermarkapp.service;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.photowatermarkapp.config.StorageProperties;
import com.photowatermarkapp.model.ExportConfig;
import com.photowatermarkapp.model.ImageWatermarkConfig;
import com.photowatermarkapp.model.LayoutConfig;
import com.photowatermarkapp.model.NamingRule;
import com.photowatermarkapp.model.ResizeConfig;
import com.photowatermarkapp.model.ShadowStyle;
import com.photowatermarkapp.model.StrokeStyle;
import com.photowatermarkapp.model.TextWatermarkConfig;
import com.photowatermarkapp.model.WatermarkConfig;
import com.photowatermarkapp.model.export.ExportFileResult;
import com.photowatermarkapp.model.export.ExportJob;
import com.photowatermarkapp.model.export.ExportJobStatus;
import com.photowatermarkapp.model.export.ExportJobView;
import com.photowatermarkapp.model.export.ExportRequest;
import com.photowatermarkapp.util.ColorUtils;

import jakarta.annotation.PreDestroy;

@Service
public class ExportService {

    private static final Map<String, double[]> PRESET_POSITIONS = Map.ofEntries(
            Map.entry("top-left", new double[] { 0.1, 0.15 }),
            Map.entry("top-center", new double[] { 0.5, 0.15 }),
            Map.entry("top-right", new double[] { 0.9, 0.15 }),
            Map.entry("center-left", new double[] { 0.15, 0.5 }),
            Map.entry("center", new double[] { 0.5, 0.5 }),
            Map.entry("center-right", new double[] { 0.85, 0.5 }),
            Map.entry("bottom-left", new double[] { 0.15, 0.85 }),
            Map.entry("bottom-center", new double[] { 0.5, 0.9 }),
            Map.entry("bottom-right", new double[] { 0.85, 0.9 }));

    private final StorageProperties storageProperties;
    private final ExecutorService executor;

    static {
        ImageIO.scanForPlugins();
    }
    private final Map<String, ExportJob> jobs = new ConcurrentHashMap<>();

    public ExportService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        int cpu = Math.max(2, Runtime.getRuntime().availableProcessors());
        this.executor = Executors.newFixedThreadPool(cpu);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    public ExportJob submitExport(List<MultipartFile> files, ExportRequest request) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one image must be uploaded");
        }
        ExportConfig exportConfig = Optional.ofNullable(request.getExportConfig())
                .orElseThrow(() -> new IllegalArgumentException("Export configuration must be provided"));

        Path outputDir = resolveOutputDirectory(exportConfig.getOutputDir());
        ensureDirectory(outputDir);

        String jobId = UUID.randomUUID().toString();
        ExportJob job = new ExportJob(jobId);
        job.setOutputDirectory(outputDir.toString());
        job.setTotalFiles(files.size());
        jobs.put(jobId, job);

        executor.submit(() -> processJob(job, files, request, outputDir));
        return job;
    }

    public Optional<ExportJobView> findJob(String jobId) {
        return Optional.ofNullable(jobs.get(jobId)).map(ExportJobView::from);
    }

    public List<ExportJobView> listJobs() {
        return jobs.values().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(ExportJobView::from)
                .toList();
    }

    public boolean cancelJob(String jobId) {
        ExportJob job = jobs.get(jobId);
        if (job == null) {
            return false;
        }
        ExportJobStatus status = job.getStatus();
        if (status == ExportJobStatus.COMPLETED || status == ExportJobStatus.FAILED
                || status == ExportJobStatus.CANCELLED) {
            return false;
        }
        job.requestCancel();
        job.setStatus(ExportJobStatus.CANCELLED);
        job.setMessage("Export job cancelled");
        return true;
    }

    private void processJob(ExportJob job, List<MultipartFile> files, ExportRequest request, Path outputDir) {
        job.setStatus(ExportJobStatus.RUNNING);
        WatermarkConfig watermarkConfig = request.getWatermarkConfig();
        ExportConfig exportConfig = request.getExportConfig();

        for (MultipartFile file : files) {
            if (job.isCancelRequested()) {
                job.setStatus(ExportJobStatus.CANCELLED);
                job.setMessage("Export cancelled after processing " + job.getProcessedFiles() + " / "
                        + job.getTotalFiles());
                return;
            }

            String originalName = FilenameUtils.getName(file.getOriginalFilename());
            job.setCurrentFile(originalName);

            ExportFileResult result = new ExportFileResult();
            result.setSourceName(originalName);

            try (InputStream inputStream = file.getInputStream()) {
                BufferedImage inputImage = ImageIO.read(inputStream);
                if (inputImage == null) {
                    throw new IOException("Unable to read image");
                }

                BufferedImage processed = applyResizeIfNeeded(inputImage, exportConfig.getResize());
                applyWatermark(processed, watermarkConfig);

                String format = normalizeFormat(exportConfig.getFormat());
                Path outputFile = buildOutputPath(outputDir, originalName, format, exportConfig.getNaming());
                writeImage(processed, format, exportConfig, outputFile);

                result.setOutputName(outputFile.getFileName().toString());
                result.setSuccess(true);
                job.incrementSuccess();
            } catch (Exception ex) {
                result.setSuccess(false);
                result.setMessage(ex.getMessage());
                job.incrementFailure();
            }

            job.addResult(result);
            job.incrementProcessed();
        }

        job.setCurrentFile(null);
        if (job.getFailureCount() > 0) {
            job.setStatus(ExportJobStatus.COMPLETED);
            job.setMessage("Completed with " + job.getFailureCount() + " failed file(s)");
        } else {
            job.setStatus(ExportJobStatus.COMPLETED);
            job.setMessage("All files exported successfully");
        }
    }

    private Path resolveOutputDirectory(String configuredPath) {
        String timestamp = Instant.now().toString().replace(':', '-');
        if (!StringUtils.hasText(configuredPath)) {
            return storageProperties.resolve("exports", timestamp);
        }
        Path base = Path.of(configuredPath).toAbsolutePath().normalize();
        return base.resolve("photo-watermark-" + timestamp);
    }

    private void ensureDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create output directory: " + directory, ex);
        }
    }

    private String normalizeFormat(String format) {
        String value = StringUtils.hasText(format) ? format.toLowerCase(Locale.ROOT) : "png";
        if (!Set.of("png", "jpeg", "jpg").contains(value)) {
            return "png";
        }
        return value.equals("jpg") ? "jpeg" : value;
    }

    private BufferedImage applyResizeIfNeeded(BufferedImage source, ResizeConfig resize) {
        if (resize == null || !StringUtils.hasText(resize.getMode())) {
            return source;
        }
        String mode = resize.getMode().toLowerCase(Locale.ROOT);
        int width = source.getWidth();
        int height = source.getHeight();
        int targetWidth = width;
        int targetHeight = height;

        switch (mode) {
            case "w" -> {
                if (resize.getWidth() != null && resize.getWidth() > 0) {
                    targetWidth = resize.getWidth();
                    targetHeight = (int) Math.round(height * (targetWidth / (double) width));
                }
            }
            case "h" -> {
                if (resize.getHeight() != null && resize.getHeight() > 0) {
                    targetHeight = resize.getHeight();
                    targetWidth = (int) Math.round(width * (targetHeight / (double) height));
                }
            }
            case "pct" -> {
                if (resize.getPercent() != null) {
                    double pct = resize.getPercent();
                    targetWidth = (int) Math.round(width * pct / 100.0);
                    targetHeight = (int) Math.round(height * pct / 100.0);
                }
            }
            default -> {
            }
        }

        targetWidth = Math.max(1, targetWidth);
        targetHeight = Math.max(1, targetHeight);
        if (targetWidth == width && targetHeight == height) {
            return source;
        }

        BufferedImage output = new BufferedImage(targetWidth, targetHeight,
                source.getTransparency() == BufferedImage.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = output.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return output;
    }

private void applyWatermark(BufferedImage image, WatermarkConfig config) {
        if (config == null || !StringUtils.hasText(config.getType())) {
            return;
        }
        if ("image".equalsIgnoreCase(config.getType())) {
            applyImageWatermark(image, config);
            return;
        }
        if ("text".equalsIgnoreCase(config.getType())) {
            applyTextWatermark(image, config);
        }
    }

    private void applyTextWatermark(BufferedImage image, WatermarkConfig config) {
        TextWatermarkConfig textConfig = config.getText();
        if (textConfig == null || !StringUtils.hasText(textConfig.getContent())) {
            return;
        }

        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int fontStyle = Font.PLAIN;
            if (Boolean.TRUE.equals(textConfig.getBold())) {
                fontStyle |= Font.BOLD;
            }
            if (Boolean.TRUE.equals(textConfig.getItalic())) {
                fontStyle |= Font.ITALIC;
            }
            int baseSize = textConfig.getFontSize() != null ? textConfig.getFontSize() : 32;
            if (config.getLayout() != null && config.getLayout().getScale() != null) {
                baseSize = Math.max(8, (int) Math.round(baseSize * config.getLayout().getScale()));
            }
            Font font = new Font(Optional.ofNullable(textConfig.getFontFamily()).orElse(Font.SANS_SERIF), fontStyle,
                    baseSize);
            g2d.setFont(font);

            FontMetrics metrics = g2d.getFontMetrics();
            int textWidth = metrics.stringWidth(textConfig.getContent());

            double[] anchor = resolveAnchor(config.getLayout(), image.getWidth(), image.getHeight());
            double anchorX = anchor[0];
            double anchorY = anchor[1];

            float drawX = (float) (anchorX - textWidth / 2.0);
            float drawY = (float) (anchorY + metrics.getAscent() / 2.0);

            AffineTransform backup = g2d.getTransform();
            double rotation = config.getLayout() != null && config.getLayout().getRotationDeg() != null
                    ? Math.toRadians(config.getLayout().getRotationDeg())
                    : 0;
            if (rotation != 0) {
                g2d.rotate(rotation, anchorX, anchorY);
            }

            float opacity = Optional.ofNullable(textConfig.getOpacity()).map(v -> v.floatValue() / 100f).orElse(0.8f);
            opacity = Math.max(0f, Math.min(1f, opacity));
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

            ShadowStyle shadow = textConfig.getShadow();
            if (shadow != null) {
                Color shadowColor = ColorUtils.parseColor(shadow.getColor(), new Color(0f, 0f, 0f, 0.4f));
                double offsetX = Optional.ofNullable(shadow.getOffsetX()).orElse(2.0);
                double offsetY = Optional.ofNullable(shadow.getOffsetY()).orElse(2.0);
                g2d.setColor(shadowColor);
                g2d.drawString(textConfig.getContent(), drawX + (float) offsetX, drawY + (float) offsetY);
            }

            StrokeStyle stroke = textConfig.getStroke();
            if (stroke != null && stroke.getWidth() != null && stroke.getWidth() > 0) {
                Color strokeColor = ColorUtils.parseColor(stroke.getColor(), Color.BLACK);
                g2d.setColor(strokeColor);
                GlyphVector glyphVector = font.createGlyphVector(g2d.getFontRenderContext(), textConfig.getContent());
                Shape shape = glyphVector.getOutline(drawX, drawY);
                g2d.setStroke(new BasicStroke(stroke.getWidth().floatValue()));
                g2d.draw(shape);
            }

            Color fill = ColorUtils.parseColor(textConfig.getColor(), Color.WHITE);
            g2d.setColor(fill);
            g2d.drawString(textConfig.getContent(), drawX, drawY);

            g2d.setTransform(backup);
        } finally {
            g2d.dispose();
        }
    }

    private void applyImageWatermark(BufferedImage image, WatermarkConfig config) {
        ImageWatermarkConfig imageConfig = config.getImage();
        if (imageConfig == null || !StringUtils.hasText(imageConfig.getData())) {
            return;
        }

        String data = imageConfig.getData();
        String base64 = data;
        int commaIndex = data.indexOf(',');
        if (commaIndex >= 0) {
            base64 = data.substring(commaIndex + 1);
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException ex) {
            return;
        }

        BufferedImage watermark;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            watermark = ImageIO.read(bais);
        } catch (IOException ex) {
            return;
        }

        if (watermark == null) {
            return;
        }

        double scale = Optional.ofNullable(imageConfig.getScale()).orElse(0.3);
        scale = Math.max(0.05, Math.min(1.0, scale));
        int targetWidth = (int) Math.round(image.getWidth() * scale);
        if (targetWidth <= 0) {
            return;
        }
        double ratio = targetWidth / (double) watermark.getWidth();
        int targetHeight = (int) Math.round(watermark.getHeight() * ratio);
        if (targetHeight <= 0) {
            return;
        }

        double[] anchor = resolveAnchor(config.getLayout(), image.getWidth(), image.getHeight());
        double anchorX = anchor[0];
        double anchorY = anchor[1];
        int drawX = (int) Math.round(anchorX - targetWidth / 2.0);
        int drawY = (int) Math.round(anchorY - targetHeight / 2.0);

        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            float opacity = Optional.ofNullable(imageConfig.getOpacity()).map(v -> v.floatValue() / 100f).orElse(0.8f);
            opacity = Math.max(0f, Math.min(1f, opacity));
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            g2d.drawImage(watermark, drawX, drawY, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }
    }
    private double[] resolveAnchor(LayoutConfig layout, int width, int height) {
        double relativeX = 0.5;
        double relativeY = 0.85;
        if (layout != null) {
            if (layout.getPreset() != null && PRESET_POSITIONS.containsKey(layout.getPreset())) {
                double[] preset = PRESET_POSITIONS.get(layout.getPreset());
                relativeX = preset[0];
                relativeY = preset[1];
            }
            if (layout.getX() != null) {
                relativeX = clamp(layout.getX(), 0.0, 1.0);
            }
            if (layout.getY() != null) {
                relativeY = clamp(layout.getY(), 0.0, 1.0);
            }
        }
        return new double[] { relativeX * width, relativeY * height };
    }

    private double clamp(Double value, double min, double max) {
        if (value == null) {
            return min;
        }
        return Math.min(max, Math.max(min, value));
    }

    private Path buildOutputPath(Path outputDir, String originalName, String format, NamingRule namingRule)
            throws IOException {
        String baseName = FilenameUtils.getBaseName(originalName);
        String extension = format.equals("jpeg") ? "jpg" : format;

        StringBuilder builder = new StringBuilder();
        if (namingRule != null && StringUtils.hasText(namingRule.getPrefix())) {
            builder.append(namingRule.getPrefix());
        }
        builder.append(baseName);
        if (namingRule != null && StringUtils.hasText(namingRule.getSuffix())) {
            builder.append(namingRule.getSuffix());
        }

        String candidate = builder.toString();
        Path target = outputDir.resolve(candidate + "." + extension);
        int index = 1;
        while (Files.exists(target)) {
            target = outputDir.resolve(candidate + "-" + index + "." + extension);
            index++;
        }
        return target;
    }

    private void writeImage(BufferedImage image, String format, ExportConfig exportConfig, Path outputFile)
            throws IOException {
        if ("jpeg".equals(format)) {
            BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = rgbImage.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();

            float quality = Optional.ofNullable(exportConfig.getJpegQuality())
                    .map(v -> Math.max(0, Math.min(100, v)) / 100f)
                    .orElse(0.92f);

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(quality);
                }
                try (ImageOutputStream ios = ImageIO.createImageOutputStream(Files.newOutputStream(outputFile,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
                    writer.setOutput(ios);
                    writer.write(null, new IIOImage(rgbImage, null, null), param);
                } finally {
                    writer.dispose();
                }
            } else {
                ImageIO.write(rgbImage, "jpeg", outputFile.toFile());
            }
        } else {
            ImageIO.write(image, format, outputFile.toFile());
        }
    }
}
