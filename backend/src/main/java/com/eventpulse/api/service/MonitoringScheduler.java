package com.eventpulse.api.service;

import com.eventpulse.api.entity.CheckLog;
import com.eventpulse.api.entity.MonitoredEndpoint;
import com.eventpulse.api.repository.CheckLogRepository;
import com.eventpulse.api.repository.MonitoredEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringScheduler {

    private final MonitoredEndpointRepository endpointRepository;
    private final CheckLogRepository checkLogRepository;
    private final WebhookDispatcherService webhookDispatcherService;

    @Autowired
    private CacheManager cacheManager;

    private void updateAndEvictCache(MonitoredEndpoint endpoint) {
        endpointRepository.save(endpoint);

        // Evict cached endpoints list in Redis so polling gets updated status
        if (cacheManager.getCache("endpoints") != null) {
            cacheManager.getCache("endpoints").clear();
        }
    }

    // Standard JDK 11+ HttpClient for async HTTP requests
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // Runs every 10 seconds to check active endpoints
    @Scheduled(fixedRate = 10000)
    public void runHealthChecks() {
        List<MonitoredEndpoint> endpoints = endpointRepository.findByActiveTrue();

        for (MonitoredEndpoint endpoint : endpoints) {
            CompletableFuture.runAsync(() -> pingEndpoint(endpoint));
        }
    }
    private void pingEndpoint(MonitoredEndpoint endpoint) {
        long startTime = System.currentTimeMillis();
        boolean isSuccess;
        int statusCode;
        String errorMessage = null;

        // Capture the status before this check to detect state transitions
        String previousStatus = endpoint.getLastStatus();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint.getUrl()))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            statusCode = response.statusCode();
            long duration = System.currentTimeMillis() - startTime;

            isSuccess = (statusCode == endpoint.getExpectedStatusCode());
            String currentStatus = isSuccess ? "UP" : "DOWN";

            // 1. Update endpoint state
            endpoint.setLastCheckedAt(LocalDateTime.now());
            endpoint.setLastStatus(currentStatus);
            endpointRepository.save(endpoint);

            // 2. Trigger webhook alert if status transitioned (e.g. UP -> DOWN or DOWN -> UP)
            if (previousStatus != null && !previousStatus.equals(currentStatus)) {
                String eventType = "UP".equals(currentStatus) ? "ENDPOINT_RECOVERED" : "ENDPOINT_DOWN";
                webhookDispatcherService.dispatchAlert(
                        endpoint.getWebhookUrl(),
                        eventType,
                        endpoint.getName(),
                        endpoint.getUrl(),
                        statusCode,
                        currentStatus
                );
            }

            // 3. Log historical check entry
            CheckLog logEntry = CheckLog.builder()
                    .endpoint(endpoint)
                    .statusCode(statusCode)
                    .responseTimeMs(duration)
                    .success(isSuccess)
                    .errorMessage(isSuccess ? null : "Unexpected status code: " + statusCode)
                    .build();

            checkLogRepository.save(logEntry);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to ping endpoint {}: {}", endpoint.getUrl(), e.getMessage());

            String currentStatus = "DOWN";

            // 1. Update endpoint state
            endpoint.setLastCheckedAt(LocalDateTime.now());
            endpoint.setLastStatus(currentStatus);
            endpointRepository.save(endpoint);

            // 2. Trigger webhook alert if status transitioned to DOWN
            if (previousStatus != null && !"DOWN".equals(previousStatus)) {
                webhookDispatcherService.dispatchAlert(
                        endpoint.getWebhookUrl(),
                        "ENDPOINT_DOWN",
                        endpoint.getName(),
                        endpoint.getUrl(),
                        0,
                        currentStatus
                );
            }

            // 3. Log historical check entry
            CheckLog logEntry = CheckLog.builder()
                    .endpoint(endpoint)
                    .statusCode(0)
                    .responseTimeMs(duration)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();

            checkLogRepository.save(logEntry);
        } finally {
            // Evict stale endpoints list in Redis so polling picks up the new status
            if (cacheManager.getCache("endpoints") != null) {
                cacheManager.getCache("endpoints").clear();
            }
        }
    }

    public void triggerManualPing(MonitoredEndpoint endpoint) {
        CompletableFuture.runAsync(() -> pingEndpoint(endpoint));
    }
}