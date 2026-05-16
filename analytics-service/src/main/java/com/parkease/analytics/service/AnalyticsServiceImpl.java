package com.parkease.analytics.service;

import com.parkease.analytics.client.AuthServiceClient;
import com.parkease.analytics.client.BookingServiceClient;
import com.parkease.analytics.client.ParkingLotServiceClient;
import com.parkease.analytics.client.PaymentServiceClient;
import com.parkease.analytics.client.SpotServiceClient;
import com.parkease.analytics.dto.response.*;
import com.parkease.analytics.entity.OccupancyLog;
import com.parkease.analytics.entity.AuditLog;
import com.parkease.analytics.repository.OccupancyLogRepository;
import com.parkease.analytics.repository.AuditLogRepository;
import com.parkease.analytics.dto.request.AuditLogRequestDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OccupancyLogRepository logRepo;
    private final AuditLogRepository auditRepo;
    private final BookingServiceClient   bookingServiceClient;
    private final PaymentServiceClient   paymentServiceClient;
    private final AuthServiceClient      authServiceClient;
    private final ParkingLotServiceClient parkingLotServiceClient;
    private final SpotServiceClient       spotServiceClient;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public OccupancyRateDTO getOccupancyRate(Long lotId) {
        log.debug("Getting real-time occupancy rate for lot: {}", lotId);

        try {
            Map<String, Object> lotData = parkingLotServiceClient.getLotById(lotId);
            if (lotData != null && lotData.get("totalSpots") != null && lotData.get("availableSpots") != null) {
                int totalSpots = ((Number) lotData.get("totalSpots")).intValue();
                int availableSpots = ((Number) lotData.get("availableSpots")).intValue();
                int occupiedSpots = totalSpots - availableSpots;
                
                double rate = totalSpots > 0 ? (double) occupiedSpots / totalSpots : 0.0;
                double percent = Math.round(rate * 100.0 * 100.0) / 100.0;

                return OccupancyRateDTO.builder()
                        .lotId(lotId)
                        .occupiedSpots(occupiedSpots)
                        .totalSpots(totalSpots)
                        .occupancyRate(Math.round(rate * 10000.0) / 10000.0)
                        .occupancyPercent(percent)
                        .availableSpots(availableSpots)
                        .build();
            }
        } catch (Exception e) {
            log.warn("Could not fetch real-time lot data, falling back to occupancy logs: {}", e.getMessage());
        }

        // Fallback to logs if service call fails
        OccupancyLog latest = logRepo.findTopByLotIdOrderByTimestampDesc(lotId);

        if (latest == null) {
            return OccupancyRateDTO.builder()
                    .lotId(lotId)
                    .occupiedSpots(0).totalSpots(0)
                    .occupancyRate(0.0).occupancyPercent(0.0)
                    .availableSpots(0).build();
        }

        double percent = Math.round(latest.getOccupancyRate() * 100.0 * 100.0) / 100.0;

        return OccupancyRateDTO.builder()
                .lotId(lotId)
                .occupiedSpots(latest.getOccupiedSpots())
                .totalSpots(latest.getTotalSpots())
                .occupancyRate(latest.getOccupancyRate())
                .occupancyPercent(percent)
                .availableSpots(latest.getTotalSpots() - latest.getOccupiedSpots())
                .build();
    }

    @Override
    public Map<Integer, Double> getHourlyOccupancy(Long lotId) {
        log.debug("Getting hourly occupancy for lot: {}", lotId);

        List<Object[]> results = logRepo.getHourlyOccupancy(lotId);
        Map<Integer, Double> hourlyMap = new LinkedHashMap<>();

        for (int h = 0; h < 24; h++) hourlyMap.put(h, 0.0);

        for (Object[] row : results) {
            int hour      = ((Number) row[0]).intValue();
            double avgRate = ((Number) row[1]).doubleValue();
            hourlyMap.put(hour, Math.round(avgRate * 100.0) / 100.0);
        }

        return hourlyMap;
    }

    @Override
    public List<Integer> getPeakHours(Long lotId, int topN) {
        log.debug("Getting peak hours for lot: {} (top {})", lotId, topN);

        List<Object[]> results = logRepo.getPeakHours(lotId);

        return results.stream()
                .limit(topN)
                .map(row -> ((Number) row[0]).intValue())
                .collect(Collectors.toList());
    }

    @Override
    public RevenueReportDTO getRevenueReport(Long lotId, LocalDate from, LocalDate to, String token) {
        log.debug("Getting revenue report for lot: {} from {} to {}", lotId, from, to);

        List<Map<String, Object>> bookings = fetchBookingsForLot(lotId, token);

        List<Map<String, Object>> completed = bookings.stream()
                .filter(b -> "COMPLETED".equals(b.get("status")))
                .filter(b -> {
                    String createdAt = (String) b.get("createdAt");
                    if (createdAt == null) return false;
                    LocalDate date = LocalDate.parse(createdAt.substring(0, 10));
                    return !date.isBefore(from) && !date.isAfter(to);
                })
                .toList();

        Map<String, Double> revenueByDay = new TreeMap<>();
        double totalRevenue = 0.0;

        for (Map<String, Object> booking : completed) {
            String dateStr = ((String) booking.get("createdAt")).substring(0, 10);
            double amount  = booking.get("totalAmount") != null
                    ? ((Number) booking.get("totalAmount")).doubleValue() : 0.0;
            revenueByDay.merge(dateStr, amount, Double::sum);
            totalRevenue += amount;
        }

        return RevenueReportDTO.builder()
                .lotId(lotId)
                .totalRevenue(Math.round(totalRevenue * 100.0) / 100.0)
                .totalBookings(bookings.size())
                .completedBookings(completed.size())
                .revenueByDay(revenueByDay)
                .fromDate(from.format(DATE_FMT))
                .toDate(to.format(DATE_FMT))
                .build();
    }

    @Override
    public RevenueReportDTO getPlatformRevenueReport(LocalDate from, LocalDate to, String token) {
        log.debug("Getting platform-wide revenue report from {} to {}", from, to);

        List<Map<String, Object>> allBookings = fetchAllBookings(token);

        List<Map<String, Object>> completed = allBookings.stream()
                .filter(b -> "COMPLETED".equals(b.get("status")))
                .filter(b -> {
                    String createdAt = (String) b.get("createdAt");
                    if (createdAt == null) return false;
                    LocalDate date = LocalDate.parse(createdAt.substring(0, 10));
                    return !date.isBefore(from) && !date.isAfter(to);
                })
                .toList();

        Map<String, Double> revenueByDay = new TreeMap<>();
        double totalRevenue = 0.0;

        for (Map<String, Object> booking : completed) {
            String dateStr = ((String) booking.get("createdAt")).substring(0, 10);
            double amount  = booking.get("totalAmount") != null
                    ? ((Number) booking.get("totalAmount")).doubleValue() : 0.0;
            revenueByDay.merge(dateStr, amount, Double::sum);
            totalRevenue += amount;
        }

        return RevenueReportDTO.builder()
                .lotId(0L) // 0 or null indicates platform-wide
                .totalRevenue(Math.round(totalRevenue * 100.0) / 100.0)
                .totalBookings(allBookings.size())
                .completedBookings(completed.size())
                .revenueByDay(revenueByDay)
                .fromDate(from.format(DATE_FMT))
                .toDate(to.format(DATE_FMT))
                .build();
    }

    @Override
    public Map<String, Double> getSpotTypeUtilisation(Long lotId, String token) {
        log.debug("Getting spot type utilisation for lot: {}", lotId);

        List<Map<String, Object>> bookings = fetchBookingsForLot(lotId, token);

        Map<String, Long> countByType = new HashMap<>();
        long total = 0;

        try {
            List<Map<String, Object>> spots = spotServiceClient.getSpotsByLot(lotId);
            if (spots != null) {
                Map<Long, String> spotTypes = new HashMap<>();
                for (Map<String, Object> spot : spots) {
                    if (spot.get("spotId") != null && spot.get("vehicleType") != null) {
                        spotTypes.put(((Number) spot.get("spotId")).longValue(), (String) spot.get("vehicleType"));
                    }
                }

                for (Map<String, Object> booking : bookings) {
                    String status = (String) booking.get("status");
                    if ("COMPLETED".equals(status) || "ACTIVE".equals(status)) {
                        Long spotId = booking.get("spotId") != null ? ((Number) booking.get("spotId")).longValue() : null;
                        if (spotId != null) {
                            String type = spotTypes.get(spotId);
                            if (type != null) {
                                countByType.merge(type, 1L, Long::sum);
                                total++;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Could not fetch spots for utilisation: {}", e.getMessage());
        }

        if (total == 0) return Collections.emptyMap();

        final long finalTotal = total;
        Map<String, Double> utilisation = new LinkedHashMap<>();
        countByType.forEach((type, count) ->
            utilisation.put(type, Math.round((count * 100.0 / finalTotal) * 100.0) / 100.0)
        );

        return utilisation;
    }

    @Override
    public double getAvgParkingDuration(Long lotId, String token) {
        log.debug("Getting avg parking duration for lot: {}", lotId);

        List<Map<String, Object>> bookings = fetchBookingsForLot(lotId, token);

        List<Map<String, Object>> completed = bookings.stream()
                .filter(b -> "COMPLETED".equals(b.get("status"))
                          && b.get("checkInTime") != null
                          && b.get("checkOutTime") != null)
                .toList();

        if (completed.isEmpty()) return 0.0;

        double totalMinutes = 0;
        for (Map<String, Object> booking : completed) {
            try {
                LocalDateTime checkIn  = LocalDateTime.parse(
                        ((String) booking.get("checkInTime")).replace("Z", ""));
                LocalDateTime checkOut = LocalDateTime.parse(
                        ((String) booking.get("checkOutTime")).replace("Z", ""));
                totalMinutes += java.time.Duration.between(checkIn, checkOut).toMinutes();
            } catch (Exception e) {
                log.warn("Could not parse booking times: {}", e.getMessage());
            }
        }

        return Math.round((totalMinutes / completed.size()) * 100.0) / 100.0;
    }

    @Override
    public LotSummaryDTO getLotSummary(Long lotId, String token) {
        log.debug("Getting full summary for lot: {}", lotId);

        OccupancyRateDTO occupancy   = getOccupancyRate(lotId);
        List<Integer>    peakHours   = getPeakHours(lotId, 3);
        double           avgDuration = getAvgParkingDuration(lotId, token);
        Map<String, Double> spotUtil = getSpotTypeUtilisation(lotId, token);

        LocalDate today      = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        RevenueReportDTO todayRevenue   = getRevenueReport(lotId, today, today, token);
        RevenueReportDTO monthRevenue   = getRevenueReport(lotId, monthStart, today, token);
        RevenueReportDTO allTimeRevenue = getRevenueReport(lotId, LocalDate.of(2020, 1, 1), today, token);

        return LotSummaryDTO.builder()
                .lotId(lotId)
                .currentOccupancyRate(occupancy.getOccupancyRate())
                .occupiedSpots(occupancy.getOccupiedSpots())
                .totalSpots(occupancy.getTotalSpots())
                .revenueToday(todayRevenue.getTotalRevenue())
                .revenueThisMonth(monthRevenue.getTotalRevenue())
                .revenueAllTime(allTimeRevenue.getTotalRevenue())
                .bookingsToday(todayRevenue.getCompletedBookings())
                .bookingsThisMonth(monthRevenue.getCompletedBookings())
                .bookingsAllTime(allTimeRevenue.getTotalBookings())
                .avgParkingDurationMinutes(avgDuration)
                .peakHours(peakHours)
                .spotTypeUtilisation(spotUtil)
                .build();
    }

    @Override
    public PlatformSummaryDTO getPlatformSummary(String token) {
        log.debug("Computing platform-wide summary");

        // Use real-time lot data for occupancy instead of scheduled snapshots
        int totalLots     = 0;
        int totalSpots    = 0;
        int totalOccupied = 0;

        try {
            List<Map<String, Object>> lots = parkingLotServiceClient.getAllLots();
            if (lots != null) {
                totalLots = lots.size();
                for (Map<String, Object> lot : lots) {
                    int lotTotal     = lot.get("totalSpots")     != null ? ((Number) lot.get("totalSpots")).intValue()     : 0;
                    int lotAvailable = lot.get("availableSpots") != null ? ((Number) lot.get("availableSpots")).intValue() : 0;
                    totalSpots    += lotTotal;
                    totalOccupied += (lotTotal - lotAvailable);
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch real-time lot data, falling back to occupancy logs: {}", e.getMessage());
            // Fallback to OccupancyLog data
            List<Object[]> latestLogs = logRepo.getLatestOccupancyAllLots();
            totalLots = latestLogs.size();
            for (Object[] row : latestLogs) {
                totalOccupied += ((Number) row[2]).intValue();
                totalSpots    += ((Number) row[3]).intValue();
            }
        }

        double platformRate = totalSpots > 0
                ? Math.round((totalOccupied * 100.0 / totalSpots) * 100.0) / 100.0 : 0.0;

        // Average occupancy per lot
        double avgLotOccupancy = totalLots > 0
                ? Math.round((platformRate / totalLots) * 100.0) / 100.0 : 0.0;

        List<Map<String, Object>> allBookings = fetchAllBookings(token);

        LocalDate today      = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        long bookingsToday = allBookings.stream()
                .filter(b -> {
                    String ca = (String) b.get("createdAt");
                    return ca != null && ca.startsWith(today.format(DATE_FMT));
                }).count();

        double revenueToday     = fetchTotalRevenue(today, today, token);
        double revenueThisMonth = fetchTotalRevenue(monthStart, today, token);
        double revenueAllTime   = fetchTotalRevenue(LocalDate.of(2020, 1, 1), today, token);

        // Fetch user counts from auth-service
        int totalDrivers    = 0;
        int totalManagers   = 0;
        try {
            List<Map<String, Object>> users = authServiceClient.getAllUsers(token);
            if (users != null) {
                for (Map<String, Object> u : users) {
                    String role = (String) u.get("role");
                    if ("DRIVER".equals(role))      totalDrivers++;
                    if ("LOT_MANAGER".equals(role)) totalManagers++;
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch user counts from auth-service: {}", e.getMessage());
        }

        Map<String, Long> byCity        = new HashMap<>();
        Map<String, Long> byVehicleType = new HashMap<>();

        for (Map<String, Object> booking : allBookings) {
            String vt = (String) booking.get("vehicleType");
            if (vt != null) byVehicleType.merge(vt, 1L, Long::sum);
        }

        return PlatformSummaryDTO.builder()
                .totalActiveLots(totalLots)
                .totalSpots(totalSpots)
                .totalOccupiedSpots(totalOccupied)
                .platformOccupancyRate(platformRate)
                .averageLotOccupancyRate(avgLotOccupancy)
                .totalBookingsToday(bookingsToday)
                .totalBookingsAllTime(allBookings.size())
                .totalRevenueToday(revenueToday)
                .totalRevenueThisMonth(revenueThisMonth)
                .totalRevenueAllTime(revenueAllTime)
                .totalDrivers(totalDrivers)
                .totalLotManagers(totalManagers)
                .bookingsByCity(byCity)
                .bookingsByVehicleType(byVehicleType)
                .build();
    }

    @Override
    public void logOccupancy(Long lotId, int occupiedSpots, int totalSpots) {
        LocalDateTime now  = LocalDateTime.now();
        double rate = totalSpots > 0 ? (double) occupiedSpots / totalSpots : 0.0;

        OccupancyLog entry = OccupancyLog.builder()
                .lotId(lotId)
                .timestamp(now)
                .occupancyRate(Math.round(rate * 10000.0) / 10000.0)
                .occupiedSpots(occupiedSpots)
                .totalSpots(totalSpots)
                .hourOfDay(now.getHour())
                .dayOfWeek(now.getDayOfWeek().getValue())
                .build();

        logRepo.save(entry);
    }

    @Override
    public void logAction(AuditLogRequestDTO request, String token) {
        String performedBy = request.getPerformedBy();

        if (performedBy == null || performedBy.isBlank()) {
            performedBy = "System";
            if (token != null && !token.isBlank()) {
                try {
                    Map<String, Object> user = authServiceClient.getCurrentUser(token);
                    if (user != null && user.get("email") != null) {
                        performedBy = (String) user.get("email");
                    }
                } catch (Exception e) {
                    log.warn("Could not determine user for audit log, using System");
                }
            }
        }

        AuditLog logEntry = AuditLog.builder()
                .actionType(request.getActionType())
                .performedBy(performedBy)
                .targetId(request.getTargetId())
                .details(request.getDetails())
                .build();
                
        auditRepo.save(logEntry);
        log.info("Audit log saved: {} on {} by {}", request.getActionType(), request.getTargetId(), performedBy);
    }

    @Override
    public List<AuditLogResponseDTO> getAllAuditLogs() {
        return auditRepo.findAllByOrderByTimestampDesc().stream()
                .map(log -> AuditLogResponseDTO.builder()
                        .id(log.getId())
                        .actionType(log.getActionType())
                        .performedBy(log.getPerformedBy())
                        .targetId(log.getTargetId())
                        .details(log.getDetails())
                        .timestamp(log.getTimestamp())
                        .build())
                .toList();
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private List<Map<String, Object>> fetchBookingsForLot(Long lotId, String token) {
        try {
            List<Map<String, Object>> result = bookingServiceClient.getBookingsByLot(lotId, token);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("Could not fetch bookings for lot {}: {}", lotId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> fetchAllBookings(String token) {
        try {
            List<Map<String, Object>> result = bookingServiceClient.getAllBookings(token);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("Could not fetch all bookings: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private double fetchTotalRevenue(LocalDate from, LocalDate to, String token) {
        try {
            List<Map<String, Object>> payments = paymentServiceClient.getAllPayments(token);
            if (payments == null) return 0.0;

            return payments.stream()
                    .filter(p -> "PAID".equals(p.get("status")))
                    .filter(p -> {
                        String paidAt = (String) p.get("paidAt");
                        if (paidAt == null) return false;
                        LocalDate date = LocalDate.parse(paidAt.substring(0, 10));
                        return !date.isBefore(from) && !date.isAfter(to);
                    })
                    .mapToDouble(p -> p.get("amount") != null
                            ? ((Number) p.get("amount")).doubleValue() : 0.0)
                    .sum();
        } catch (Exception e) {
            log.error("Could not fetch revenue: {}", e.getMessage());
            return 0.0;
        }
    }
}
