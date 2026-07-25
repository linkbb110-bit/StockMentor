package com.stockmentor.system.controller;

import com.stockmentor.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<HealthPayload> health() {
        return ApiResponse.success(new HealthPayload("UP", "stockmentor-backend"));
    }

    public record HealthPayload(String status, String service) {
    }
}
