package com.parkease.analytics.controller;

import com.parkease.analytics.dto.request.AuditLogRequestDTO;
import com.parkease.analytics.dto.response.*;
import com.parkease.analytics.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService service;

    @InjectMocks
    private AnalyticsController controller;

    private MockHttpServletRequest request;
    private final String TOKEN = "Bearer test-token";

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.addHeader("Authorization", TOKEN);
    }

    @Test
    void testGetOccupancy() {
        OccupancyRateDTO mockResp = OccupancyRateDTO.builder().occupancyRate(50.0).build();
        when(service.getOccupancyRate(1L)).thenReturn(mockResp);

        ResponseEntity<OccupancyRateDTO> response = controller.getOccupancy(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(50.0, response.getBody().getOccupancyRate());
    }

    @Test
    void testGetHourly() {
        Map<Integer, Double> mockResp = Map.of(10, 80.0);
        when(service.getHourlyOccupancy(1L)).thenReturn(mockResp);

        ResponseEntity<Map<Integer, Double>> response = controller.getHourly(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(80.0, response.getBody().get(10));
    }

    @Test
    void testGetPeakHours() {
        List<Integer> mockResp = List.of(9, 10, 11);
        when(service.getPeakHours(1L, 3)).thenReturn(mockResp);

        ResponseEntity<List<Integer>> response = controller.getPeakHours(1L, 3);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(3, response.getBody().size());
    }

    @Test
    void testGetRevenue() {
        RevenueReportDTO mockResp = RevenueReportDTO.builder().totalRevenue(100.0).build();
        when(service.getRevenueReport(eq(1L), any(LocalDate.class), any(LocalDate.class), eq(TOKEN))).thenReturn(mockResp);

        ResponseEntity<RevenueReportDTO> response = controller.getRevenue(1L, LocalDate.now(), LocalDate.now(), request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(100.0, response.getBody().getTotalRevenue());
    }

    @Test
    void testGetUtilisation() {
        Map<String, Double> mockResp = Map.of("FOUR_WHEELER", 60.0);
        when(service.getSpotTypeUtilisation(1L, TOKEN)).thenReturn(mockResp);

        ResponseEntity<Map<String, Double>> response = controller.getUtilisation(1L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(60.0, response.getBody().get("FOUR_WHEELER"));
    }

    @Test
    void testGetAvgDuration() {
        when(service.getAvgParkingDuration(1L, TOKEN)).thenReturn(120.0);

        ResponseEntity<Map<String, Double>> response = controller.getAvgDuration(1L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(120.0, response.getBody().get("avgDurationMinutes"));
    }

    @Test
    void testGetLotSummary() {
        LotSummaryDTO mockResp = LotSummaryDTO.builder().totalSpots(100).build();
        when(service.getLotSummary(1L, TOKEN)).thenReturn(mockResp);

        ResponseEntity<LotSummaryDTO> response = controller.getLotSummary(1L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(100, response.getBody().getTotalSpots());
    }

    @Test
    void testGetPlatformSummary() {
        PlatformSummaryDTO mockResp = PlatformSummaryDTO.builder().totalActiveLots(5).build();
        when(service.getPlatformSummary(TOKEN)).thenReturn(mockResp);

        ResponseEntity<PlatformSummaryDTO> response = controller.getPlatformSummary(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(5, response.getBody().getTotalActiveLots());
    }

    @Test
    void testGetPlatformRevenue() {
        RevenueReportDTO mockResp = RevenueReportDTO.builder().totalRevenue(500.0).build();
        when(service.getPlatformRevenueReport(any(LocalDate.class), any(LocalDate.class), eq(TOKEN))).thenReturn(mockResp);

        ResponseEntity<RevenueReportDTO> response = controller.getPlatformRevenue(LocalDate.now(), LocalDate.now(), request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(500.0, response.getBody().getTotalRevenue());
    }

    @Test
    void testLogOccupancy() {
        ResponseEntity<Void> response = controller.logOccupancy(1L, 50, 100);

        assertEquals(200, response.getStatusCode().value());
        verify(service).logOccupancy(1L, 50, 100);
    }

    @Test
    void testLogAuditAction() {
        AuditLogRequestDTO req = new AuditLogRequestDTO();
        ResponseEntity<Void> response = controller.logAuditAction(req, request);

        assertEquals(200, response.getStatusCode().value());
        verify(service).logAction(req, TOKEN);
    }

    @Test
    void testGetAuditLogs() {
        AuditLogResponseDTO log = AuditLogResponseDTO.builder().id(1L).build();
        when(service.getAllAuditLogs()).thenReturn(List.of(log));

        ResponseEntity<List<AuditLogResponseDTO>> response = controller.getAuditLogs();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }
}
