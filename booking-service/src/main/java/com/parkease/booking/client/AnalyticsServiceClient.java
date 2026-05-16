package com.parkease.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "analytics-service", url = "http://localhost:8088")
public interface AnalyticsServiceClient {

    @PostMapping("/api/analytics/internal/audit")
    void logAuditAction(@RequestBody Map<String, Object> requestDTO, @RequestHeader("Authorization") String token);
}
