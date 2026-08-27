package com.eventpulse.api.service;

import com.eventpulse.api.dto.CreateEndpointRequest;
import com.eventpulse.api.entity.MonitoredEndpoint;
import com.eventpulse.api.repository.MonitoredEndpointRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EndpointService {

    private final MonitoredEndpointRepository endpointRepository;

    // Cache key: 'endpoints::all' - automatically returned from Redis if present
    @Cacheable(value = "endpoints", key = "'all'")
    public List<MonitoredEndpoint> getAllEndpoints() {
        return endpointRepository.findAll();
    }

    // Evict cache whenever a new endpoint is created or deleted
    @CacheEvict(value = "endpoints", allEntries = true)
    public MonitoredEndpoint createEndpoint(@Valid CreateEndpointRequest endpoint) {
        return endpointRepository.save(endpoint);
    }

    @CacheEvict(value = "endpoints", allEntries = true)
    public void deleteEndpoint(Long id) {
        endpointRepository.deleteById(id);
    }

    // Evict endpoint cache when health status is updated by scheduler
    @CacheEvict(value = "endpoints", allEntries = true)
    public void updateEndpointStatus(MonitoredEndpoint endpoint) {
        endpointRepository.save(endpoint);
    }
}