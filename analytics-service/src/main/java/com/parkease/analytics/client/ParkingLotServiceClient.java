package com.parkease.analytics.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "PARKINGLOT-SERVICE")
public interface ParkingLotServiceClient {

    @GetMapping("/api/lots/admin/all")
    List<Map<String, Object>> getAllLots();

    @GetMapping("/api/lots/{id}")
    Map<String, Object> getLotById(@PathVariable("id") Long id);
}
