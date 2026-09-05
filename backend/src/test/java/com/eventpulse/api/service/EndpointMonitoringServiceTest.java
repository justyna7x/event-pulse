package com.eventpulse.api.service;

import com.eventpulse.api.dto.CreateEndpointRequest;
import com.eventpulse.api.entity.MonitoredEndpoint;
import com.eventpulse.api.repository.MonitoredEndpointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndpointServiceTest {

    @Mock
    private MonitoredEndpointRepository endpointRepository;

    @InjectMocks
    private EndpointService endpointService;

    @Test
    @DisplayName("createEndpoint maps record components and saves entity successfully")
    void createEndpoint_Success() {
        // Given
        CreateEndpointRequest request = new CreateEndpointRequest(
                "Auth Service",
                "https://api.auth.com/health",
                "GET",
                200,
                30
        );

        MonitoredEndpoint savedEntity = new MonitoredEndpoint();
        savedEntity.setId(1L);
        savedEntity.setName(request.name());

        when(endpointRepository.save(any(MonitoredEndpoint.class))).thenReturn(savedEntity);

        // When
        MonitoredEndpoint result = endpointService.createEndpoint(request);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Auth Service", result.getName());
        verify(endpointRepository, times(1)).save(any(MonitoredEndpoint.class));
    }

    @Test
    @DisplayName("deleteEndpoint triggers repository deletion when ID exists")
    void deleteEndpoint_Success() {
        // Given
        Long endpointId = 5L;
        when(endpointRepository.existsById(endpointId)).thenReturn(true);

        // When
        endpointService.deleteEndpoint(endpointId);

        // Then
        verify(endpointRepository, times(1)).existsById(endpointId);
        verify(endpointRepository, times(1)).deleteById(endpointId);
    }

    @Test
    @DisplayName("deleteEndpoint throws exception when ID does not exist")
    void deleteEndpoint_NotFound_ThrowsException() {
        // Given
        Long nonExistentId = 99L;
        when(endpointRepository.existsById(nonExistentId)).thenReturn(false);

        // When / Then
        assertThrows(IllegalArgumentException.class, () ->
                endpointService.deleteEndpoint(nonExistentId)
        );

        verify(endpointRepository, never()).deleteById(anyLong());
    }
}