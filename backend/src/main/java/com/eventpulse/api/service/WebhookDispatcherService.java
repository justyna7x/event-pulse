package com.eventpulse.api.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
public class WebhookDispatcherService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Data
    @Builder
    public static class AlertPayload {
        private String eventType;       // e.g., "ENDPOINT_DOWN", "ENDPOINT_RECOVERED"
        private String endpointName;
        private String targetUrl;
        private int statusCode;
        private String status;
        private String timestamp;
    }

    @Async
    public void dispatchAlert(String webhookUrl, String eventType, String name, String targetUrl, int statusCode, String status) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        try {
            AlertPayload payload = AlertPayload.builder()
                    .eventType(eventType)
                    .endpointName(name)
                    .targetUrl(targetUrl)
                    .statusCode(statusCode)
                    .status(status)
                    .timestamp(LocalDateTime.now().toString())
                    .build();

            // Simple JSON construction (or use Jackson ObjectMapper)
            String jsonBody = String.format(
                    "{\"eventType\":\"%s\",\"endpointName\":\"%s\",\"targetUrl\":\"%s\",\"statusCode\":%d,\"status\":\"%s\",\"timestamp\":\"%s\"}",
                    payload.getEventType(),
                    payload.getEndpointName(),
                    payload.getTargetUrl(),
                    payload.getStatusCode(),
                    payload.getStatus(),
                    payload.getTimestamp()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            log.info("Dispatching webhook alert [{}] for {} to {}", eventType, name, webhookUrl);
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(res -> log.info("Webhook delivered with HTTP status {}", res.statusCode()))
                    .exceptionally(ex -> {
                        log.error("Failed to deliver webhook to {}: {}", webhookUrl, ex.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            log.error("Error creating webhook payload for {}: {}", name, e.getMessage());
        }
    }
}