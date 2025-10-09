package com.photowatermarkapp;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
        if (!Desktop.isDesktopSupported()) {
            log.info("Desktop operations not supported on this platform; skip auto opening browser.");
            return;
        }
        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
        String host = environment.getProperty("app.desktop.host", "127.0.0.1");
        URI target;
        try {
            target = new URI("http://" + host + ":" + port);
        } catch (URISyntaxException e) {
            log.warn("Unable to build URI for auto open", e);
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                Desktop.getDesktop().browse(target);
                log.info("Opened default browser at {}", target);
            } catch (IOException e) {
                log.warn("Failed to open default browser", e);
            }
        });
    }
}
