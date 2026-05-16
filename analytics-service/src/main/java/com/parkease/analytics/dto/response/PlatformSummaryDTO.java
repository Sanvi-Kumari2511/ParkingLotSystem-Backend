package com.parkease.analytics.dto.response;

import lombok.*;
import java.util.Map;


@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class PlatformSummaryDTO {
    private int totalActiveLots;
    private int totalSpots;
    private int totalOccupiedSpots;
    private double platformOccupancyRate;
    private double averageLotOccupancyRate;
    private long totalBookingsToday;
    private long totalBookingsAllTime;
    private double totalRevenueToday;
    private double totalRevenueThisMonth;
    private double totalRevenueAllTime;
    private int totalDrivers;
    private int totalLotManagers;

    private Map<String, Long> bookingsByCity;
    private Map<String, Long> bookingsByVehicleType;
}
