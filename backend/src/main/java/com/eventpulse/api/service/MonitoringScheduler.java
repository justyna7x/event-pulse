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

            // Update endpoint status
            endpoint.setLastCheckedAt(LocalDateTime.now());
            endpoint.setLastStatus(isSuccess ? "UP" : "DOWN");
            endpointRepository.save(endpoint);

            // Log entry
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

            endpoint.setLastCheckedAt(LocalDateTime.now());
            endpoint.setLastStatus("DOWN");
            endpointRepository.save(endpoint);

            CheckLog logEntry = CheckLog.builder()
                    .endpoint(endpoint)
                    .statusCode(0)
                    .responseTimeMs(duration)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();

            checkLogRepository.save(logEntry);
        }
    }
}