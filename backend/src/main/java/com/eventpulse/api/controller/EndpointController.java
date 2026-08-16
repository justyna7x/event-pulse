package com.eventpulse.api.controller;

import com.eventpulse.api.dto.CreateEndpointRequest;
import com.eventpulse.api.entity.MonitoredEndpoint;
import com.eventpulse.api.service.EndpointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}