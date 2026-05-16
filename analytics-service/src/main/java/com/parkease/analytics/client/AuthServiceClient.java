package com.parkease.analytics.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@FeignClient(name = "AUTH-SERVICE")
public interface AuthServiceClient {

    @GetMapping("/api/admin/users")
    List<Map<String, Object>> getAllUsers(
            @RequestHeader("Authorization") String authorization);

    @GetMapping("/api/auth/me")
    Map<String, Object> getCurrentUser(
            @RequestHeader("Authorization") String authorization);
}
