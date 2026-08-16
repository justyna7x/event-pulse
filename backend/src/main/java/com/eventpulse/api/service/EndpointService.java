package com.eventpulse.api.service;

import com.eventpulse.api.dto.CreateEndpointRequest;
import com.eventpulse.api.entity.MonitoredEndpoint;
import com.eventpulse.api.repository.MonitoredEndpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EndpointService {

    private final MonitoredEndpointRepository endpointRepository;

    public List<MonitoredEndpoint> getAllEndpoints() {
        return endpointRepository.findAll();
    }

    public MonitoredEndpoint createEndpoint(CreateEndpointRequest request) {
        MonitoredEndpoint endpoint = MonitoredEndpoint.builder()
                .name(request.name())
                .url(request.url())
                .httpMethod(request.httpMethod() != null ? request.httpMethod() : "GET")
                .expectedStatusCode(request.expectedStatusCode() != null ? request.expectedStatusCode() : 200)
                .checkIntervalSeconds(request.checkIntervalSeconds() != null ? request.checkIntervalSeconds() : 60)
                .active(true)
                .build();

        return endpointRepository.save(endpoint);
    }

    public void deleteEndpoint(Long id) {
        endpointRepository.deleteById(id);
    }
}