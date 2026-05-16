package com.parkease.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "VEHICLE-SERVICE")
public interface VehicleServiceClient {

    @GetMapping("/api/vehicles/plate/{plate}")
    Map<String, Object> getVehicleByPlate(@PathVariable("plate") String plate);
}
