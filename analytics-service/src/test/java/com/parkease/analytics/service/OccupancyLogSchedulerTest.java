package com.parkease.analytics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.parkease.analytics.client.*;
import java.util.Arrays;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OccupancyLogSchedulerTest {

    @Mock private AnalyticsService analyticsService;
    @Mock private BookingServiceClient bookingServiceClient;
    @Mock private SpotServiceClient spotServiceClient;

    @InjectMocks
    private OccupancyLogScheduler scheduler;

    @BeforeEach
    void setUp() {
    }

    @Test
    void shouldLogAllLotOccupanciesSuccessfully() {
        Map[] bookings = new Map[]{
                Map.of("lotId", 1L),
                Map.of("lotId", 2L)
        };

        Map[] spotsLot1 = new Map[]{
                Map.of("spotId", 1),
                Map.of("spotId", 2),
                Map.of("spotId", 3)
        };

        Map[] spotsLot2 = new Map[]{
                Map.of("spotId", 1),
                Map.of("spotId", 2)
        };

        when(bookingServiceClient.getDistinctLotIds()).thenReturn(Arrays.asList(1L, 2L));

        when(spotServiceClient.getAvailableSpotCount(1L)).thenReturn(1);

        when(spotServiceClient.getSpotsByLot(1L)).thenReturn(Arrays.asList(spotsLot1));

        when(spotServiceClient.getAvailableSpotCount(2L)).thenReturn(1);

        when(spotServiceClient.getSpotsByLot(2L)).thenReturn(Arrays.asList(spotsLot2));

        scheduler.logAllLotOccupancies();

        verify(analyticsService).logOccupancy(1L, 2, 3);
        verify(analyticsService).logOccupancy(2L, 1, 2);
    }

    @Test
    void shouldDoNothingWhenNoActiveLotsFound() {
        when(bookingServiceClient.getDistinctLotIds()).thenReturn(Arrays.asList());

        scheduler.logAllLotOccupancies();

        verify(analyticsService, never())
                .logOccupancy(anyLong(), anyInt(), anyInt());
    }

    @Test
    void shouldContinueWhenOneLotFails() {
        Map[] bookings = new Map[]{
                Map.of("lotId", 1L),
                Map.of("lotId", 2L)
        };

        Map[] spotsLot2 = new Map[]{
                Map.of("spotId", 1),
                Map.of("spotId", 2)
        };

        when(bookingServiceClient.getDistinctLotIds()).thenReturn(Arrays.asList(1L, 2L));

        when(spotServiceClient.getAvailableSpotCount(1L)).thenThrow(new RuntimeException("spot service down"));

        when(spotServiceClient.getAvailableSpotCount(2L)).thenReturn(1);

        when(spotServiceClient.getSpotsByLot(2L)).thenReturn(Arrays.asList(spotsLot2));

        scheduler.logAllLotOccupancies();

        verify(analyticsService).logOccupancy(2L, 1, 2);
    }

    @Test
    void shouldHandleBookingServiceFailureGracefully() {
        when(bookingServiceClient.getDistinctLotIds()).thenThrow(new RuntimeException("booking service down"));

        scheduler.logAllLotOccupancies();

        verify(analyticsService, never())
                .logOccupancy(anyLong(), anyInt(), anyInt());
    }
}