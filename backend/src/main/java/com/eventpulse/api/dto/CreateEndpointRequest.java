package com.eventpulse.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

public record CreateEndpointRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "URL is required")
        @URL(message = "Must be a valid URL")
        String url,

        String httpMethod,

        Integer expectedStatusCode,

        @NotNull
        @Min(value = 5, message = "Interval must be at least 5 seconds")
        Integer checkIntervalSeconds
) {}