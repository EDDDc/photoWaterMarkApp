package com.photowatermarkapp;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DesktopLauncher implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(DesktopLauncher.class);

    private final Environment environment;
    private final boolean autoOpen;

    public DesktopLauncher(Environment environment,
                           @Value("${app.desktop.auto-open:true}") boolean autoOpen) {
        this.environment = environment;
        this.autoOpen = autoOpen;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!autoOpen) {
            return;
        }
        URI target = buildTargetUri();
        if (target == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (tryDesktopOpen(target)) {
                return;
            }
            tryFallbackOpen(target);
        });
    }

    private URI buildTargetUri() {
        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
        String host = environment.getProperty("app.desktop.host", "127.0.0.1");
        try {
            return new URI("http://" + host + ":" + port);
        } catch (URISyntaxException e) {
            log.warn("Unable to build URI for auto open", e);
            return null;
        }
    }

    private boolean tryDesktopOpen(URI target) {
        if (!Desktop.isDesktopSupported()) {
            log.info("Desktop API not supported; attempting command fallback.");
            return false;
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            log.info("Desktop browse action not supported; attempting command fallback.");
            return false;
        }
        try {
            desktop.browse(target);
            log.info("Opened default browser at {} via Desktop API", target);
            return true;
        } catch (IOException e) {
            log.warn("Failed to open browser via Desktop API; attempting command fallback.", e);
            return false;
        }
    }

    private void tryFallbackOpen(URI target) {
        String os = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);
        String url = target.toString();
        ProcessBuilder builder;
        if (os.contains("win")) {
            builder = new ProcessBuilder("cmd", "/c", "start", "", url);
        } else if (os.contains("mac")) {
            builder = new ProcessBuilder("open", url);
        } else {
            builder = new ProcessBuilder("xdg-open", url);
        }
        try {
            builder.start();
            log.info("Opened browser at {} via command fallback", target);
        } catch (IOException e) {
            log.warn("Failed to open browser via command fallback", e);
        }
    }
}
