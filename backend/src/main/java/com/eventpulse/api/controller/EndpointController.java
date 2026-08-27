package com.eventpulse.api.controller;

import com.eventpulse.api.dto.CreateEndpointRequest;
import com.eventpulse.api.entity.CheckLog;
import com.eventpulse.api.entity.MonitoredEndpoint;
import com.eventpulse.api.repository.CheckLogRepository;
import com.eventpulse.api.repository.MonitoredEndpointRepository;
import com.eventpulse.api.service.EndpointService;
import com.eventpulse.api.service.MonitoringScheduler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/endpoints")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows React frontend requests locally
public class EndpointController {

    private final EndpointService endpointService;
    private final CheckLogRepository checkLogRepository;
    private final MonitoredEndpointRepository endpointRepository;
    private final MonitoringScheduler monitoringScheduler;

    @GetMapping
    public ResponseEntity<List<MonitoredEndpoint>> getAllEndpoints() {
        return ResponseEntity.ok(endpointService.getAllEndpoints());
    }

    @PostMapping
    public ResponseEntity<MonitoredEndpoint> createEndpoint(@Valid @RequestBody CreateEndpointRequest request) {
        MonitoredEndpoint created = endpointService.createEndpoint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable Long id) {
        endpointService.deleteEndpoint(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<CheckLog>> getEndpointLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") int limit) {

        Pageable pageable = PageRequest.of(0, limit);
        List<CheckLog> logs = checkLogRepository.findByEndpointIdOrderByCheckedAtDesc(id, pageable);
        return ResponseEntity.ok(logs);
    }
    @PostMapping("/{id}/ping")
    public ResponseEntity<Void> pingNow(@PathVariable Long id) {
        return endpointRepository.findById(id)
                .map(endpoint -> {
                    monitoringScheduler.triggerManualPing(endpoint);
                    return ResponseEntity.accepted().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}