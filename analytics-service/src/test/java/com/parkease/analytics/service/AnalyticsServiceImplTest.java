package com.parkease.analytics.service;

import com.parkease.analytics.dto.response.*;
import com.parkease.analytics.entity.OccupancyLog;
import com.parkease.analytics.repository.OccupancyLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import com.parkease.analytics.client.*;
import java.util.Arrays;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock private OccupancyLogRepository logRepo;
    @Mock private com.parkease.analytics.repository.AuditLogRepository auditRepo;
    @Mock private BookingServiceClient bookingServiceClient;
    @Mock private PaymentServiceClient paymentServiceClient;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private ParkingLotServiceClient parkingLotServiceClient;
    @Mock private SpotServiceClient spotServiceClient;

    @InjectMocks
    private AnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {

    }

    @Test
    void shouldReturnZeroOccupancyWhenNoLogsFound() {
        when(logRepo.findTopByLotIdOrderByTimestampDesc(1L)).thenReturn(null);

        OccupancyRateDTO result = service.getOccupancyRate(1L);

        assertNotNull(result);
        assertEquals(0.0, result.getOccupancyRate());
        assertEquals(0, result.getOccupiedSpots());
    }

    @Test
    void shouldReturnOccupancyRateSuccessfully() {
        OccupancyLog log = OccupancyLog.builder()
                .lotId(1L)
                .occupiedSpots(8)
                .totalSpots(10)
                .occupancyRate(0.8)
                .build();

        when(logRepo.findTopByLotIdOrderByTimestampDesc(1L)).thenReturn(log);

        OccupancyRateDTO result = service.getOccupancyRate(1L);

        assertEquals(0.8, result.getOccupancyRate());
        assertEquals(80.0, result.getOccupancyPercent());
        assertEquals(2, result.getAvailableSpots());
    }

    @Test
    void shouldReturnHourlyOccupancyWithDefaults() {
        List<Object[]> rows = List.of(
                new Object[]{9, 0.85},
                new Object[]{10, 0.92}
        );

        when(logRepo.getHourlyOccupancy(1L)).thenReturn(rows);

        Map<Integer, Double> result = service.getHourlyOccupancy(1L);

        assertEquals(24, result.size());
        assertEquals(0.85, result.get(9));
        assertEquals(0.92, result.get(10));
        assertEquals(0.0, result.get(0));
    }

    @Test
    void shouldReturnPeakHoursSuccessfully() {
        List<Object[]> rows = List.of(
                new Object[]{10, 0.95},
                new Object[]{11, 0.90},
                new Object[]{9, 0.88}
        );

        when(logRepo.getPeakHours(1L)).thenReturn(rows);

        List<Integer> result = service.getPeakHours(1L, 2);

        assertEquals(List.of(10, 11), result);
    }

    @Test
    void shouldReturnRevenueReportSuccessfully() {
        Map[] bookings = new Map[]{
                Map.of(
                        "status", "COMPLETED",
                        "createdAt", "2026-04-14T10:00:00",
                        "totalAmount", 150.0
                ),
                Map.of(
                        "status", "COMPLETED",
                        "createdAt", "2026-04-14T12:00:00",
                        "totalAmount", 50.0
                )
        };

        when(bookingServiceClient.getBookingsByLot(eq(1L), anyString())).thenReturn(Arrays.asList(bookings));

        RevenueReportDTO result = service.getRevenueReport(
                1L,
                LocalDate.of(2026, 4, 14),
                LocalDate.of(2026, 4, 14),
                "Bearer token"
        );

        assertEquals(200.0, result.getTotalRevenue());
        assertEquals(2, result.getCompletedBookings());
    }

    @Test
    void shouldReturnAverageParkingDurationSuccessfully() {
        Map[] bookings = new Map[]{
                Map.of(
                        "status", "COMPLETED",
                        "checkInTime", "2026-04-14T10:00:00",
                        "checkOutTime", "2026-04-14T12:00:00"
                )
        };

        when(bookingServiceClient.getBookingsByLot(eq(1L), anyString())).thenReturn(Arrays.asList(bookings));

        double result = service.getAvgParkingDuration(1L, "Bearer token");

        assertEquals(120.0, result);
    }

    @Test
    void shouldLogOccupancySuccessfully() {
        service.logOccupancy(1L, 8, 10);

        verify(logRepo).save(any(OccupancyLog.class));
    }

    @Test
    void shouldReturnPlatformSummarySuccessfully() {
        List<Object[]> latestLogs = List.of(
                new Object[]{1L, LocalDateTime.now(), 8, 10},
                new Object[]{2L, LocalDateTime.now(), 5, 10}
        );

        Map[] allBookings = new Map[]{
                Map.of(
                        "createdAt", LocalDate.now() + "T10:00:00",
                        "vehicleType", "CAR"
                )
        };

        Map[] payments = new Map[]{
                Map.of(
                        "status", "PAID",
                        "paidAt", LocalDate.now() + "T12:00:00",
                        "amount", 200.0
                )
        };


        org.mockito.Mockito.lenient().when(parkingLotServiceClient.getAllLots()).thenReturn(Arrays.asList(
            Map.of("totalSpots", 10, "availableSpots", 2),
            Map.of("totalSpots", 10, "availableSpots", 5)
        ));
        when(bookingServiceClient.getAllBookings(anyString())).thenReturn(Arrays.asList(allBookings));
        when(paymentServiceClient.getAllPayments(anyString())).thenReturn(Arrays.asList(payments));

        PlatformSummaryDTO result = service.getPlatformSummary("Bearer token");

        assertEquals(2, result.getTotalActiveLots());
        assertEquals(20, result.getTotalSpots());
        assertEquals(13, result.getTotalOccupiedSpots());
        assertEquals(200.0, result.getTotalRevenueToday());
    }

    @Test
    void shouldReturnPlatformRevenueReportSuccessfully() {
        Map[] allBookings = new Map[]{
                Map.of(
                        "status", "COMPLETED",
                        "createdAt", "2026-04-14T10:00:00",
                        "totalAmount", 150.0
                )
        };
        when(bookingServiceClient.getAllBookings(anyString())).thenReturn(Arrays.asList(allBookings));

        RevenueReportDTO result = service.getPlatformRevenueReport(
                LocalDate.of(2026, 4, 14),
                LocalDate.of(2026, 4, 14),
                "Bearer token"
        );

        assertEquals(150.0, result.getTotalRevenue());
        assertEquals(1, result.getCompletedBookings());
    }

    @Test
    void shouldReturnSpotTypeUtilisationSuccessfully() {
        Map[] bookings = new Map[]{
                Map.of("status", "COMPLETED", "spotId", 1L),
                Map.of("status", "ACTIVE", "spotId", 2L)
        };
        when(bookingServiceClient.getBookingsByLot(eq(1L), anyString())).thenReturn(Arrays.asList(bookings));
        
        List<Map<String, Object>> spots = Arrays.asList(
                Map.of("spotId", 1L, "vehicleType", "FOUR_WHEELER"),
                Map.of("spotId", 2L, "vehicleType", "TWO_WHEELER")
        );
        when(spotServiceClient.getSpotsByLot(1L)).thenReturn(spots);

        Map<String, Double> result = service.getSpotTypeUtilisation(1L, "Bearer token");

        assertEquals(50.0, result.get("FOUR_WHEELER"));
        assertEquals(50.0, result.get("TWO_WHEELER"));
    }

    @Test
    void shouldLogActionSuccessfullyWithUserFromToken() {
        com.parkease.analytics.dto.request.AuditLogRequestDTO request = new com.parkease.analytics.dto.request.AuditLogRequestDTO();
        request.setActionType("TEST_ACTION");
        request.setTargetId("123");
        
        when(authServiceClient.getCurrentUser(anyString())).thenReturn(Map.of("email", "test@example.com"));

        service.logAction(request, "Bearer token");

        verify(auditRepo).save(any(com.parkease.analytics.entity.AuditLog.class));
    }

    @Test
    void shouldReturnAllAuditLogs() {
        com.parkease.analytics.entity.AuditLog log = com.parkease.analytics.entity.AuditLog.builder()
                .actionType("LOGIN")
                .performedBy("user")
                .build();
        when(auditRepo.findAllByOrderByTimestampDesc()).thenReturn(List.of(log));

        List<com.parkease.analytics.dto.response.AuditLogResponseDTO> result = service.getAllAuditLogs();

        assertEquals(1, result.size());
        assertEquals("LOGIN", result.get(0).getActionType());
    }

    @Test
    void shouldReturnLotSummarySuccessfully() {
        // Mock getOccupancyRate dependencies
        when(parkingLotServiceClient.getLotById(1L)).thenReturn(Map.of("totalSpots", 10, "availableSpots", 2));
        
        List<Object[]> peakHoursMock = new java.util.ArrayList<>();
        peakHoursMock.add(new Object[]{10, 0.9});
        when(logRepo.getPeakHours(1L)).thenReturn(peakHoursMock);
        
        // Mock getAvgParkingDuration and getRevenueReport via getBookingsByLot
        Map[] bookings = new Map[]{
                Map.of(
                        "status", "COMPLETED",
                        "checkInTime", LocalDate.now() + "T10:00:00Z",
                        "checkOutTime", LocalDate.now() + "T12:00:00Z",
                        "createdAt", LocalDate.now() + "T10:00:00Z",
                        "totalAmount", 100.0,
                        "spotId", 1L
                )
        };
        when(bookingServiceClient.getBookingsByLot(eq(1L), anyString())).thenReturn(Arrays.asList(bookings));
        
        // Mock getSpotTypeUtilisation via spot service
        when(spotServiceClient.getSpotsByLot(1L)).thenReturn(List.of(Map.of("spotId", 1L, "vehicleType", "FOUR_WHEELER")));

        LotSummaryDTO result = service.getLotSummary(1L, "Bearer token");

        assertNotNull(result);
        assertEquals(0.8, result.getCurrentOccupancyRate());
        assertEquals(10, result.getTotalSpots());
        assertEquals(1, result.getBookingsToday());
        assertEquals(120.0, result.getAvgParkingDurationMinutes());
    }
}