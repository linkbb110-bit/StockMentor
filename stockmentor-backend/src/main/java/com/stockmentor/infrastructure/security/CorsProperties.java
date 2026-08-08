package com.stockmentor.infrastructure.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stockmentor.security.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        if (allowedOrigins == null
                || allowedOrigins.isEmpty()
                || allowedOrigins.stream().anyMatch(origin ->
                    origin == null || origin.isBlank() || "*".equals(origin.trim()))) {
            throw new IllegalArgumentException(
                    "CORS allowed origins must be an explicit non-empty whitelist");
        }
        allowedOrigins = allowedOrigins.stream()
                .map(String::trim)
                .toList();
    }
}
