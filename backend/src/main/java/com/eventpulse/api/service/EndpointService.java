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

    @Cacheable(value = "endpoints", key = "'all'")
    public List<MonitoredEndpoint> getAllEndpoints() {
        return endpointRepository.findAll();
    }

    @CacheEvict(value = "endpoints", allEntries = true)
    public MonitoredEndpoint createEndpoint(@Valid CreateEndpointRequest request) {
        // Access record components directly without 'get' prefix
        MonitoredEndpoint entity = new MonitoredEndpoint();
        entity.setName(request.name());
        entity.setUrl(request.url());
        entity.setHttpMethod(request.httpMethod() != null ? request.httpMethod() : "GET");
        entity.setExpectedStatusCode(request.expectedStatusCode() != null ? request.expectedStatusCode() : 200);
        entity.setCheckIntervalSeconds(request.checkIntervalSeconds());

        return endpointRepository.save(entity);
    }

    @CacheEvict(value = "endpoints", allEntries = true)
    public void deleteEndpoint(Long id) {
        endpointRepository.deleteById(id);
    }

    @CacheEvict(value = "endpoints", allEntries = true)
    public void updateEndpointStatus(MonitoredEndpoint endpoint) {
        endpointRepository.save(endpoint);
    }
}