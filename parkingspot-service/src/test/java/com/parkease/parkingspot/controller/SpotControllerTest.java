package com.parkease.parkingspot.controller;

import com.parkease.parkingspot.dto.request.BulkSpotRequestDTO;
import com.parkease.parkingspot.dto.request.SpotRequestDTO;
import com.parkease.parkingspot.dto.response.SpotResponseDTO;
import com.parkease.parkingspot.entity.SpotStatus;
import com.parkease.parkingspot.entity.SpotType;
import com.parkease.parkingspot.entity.VehicleType;
import com.parkease.parkingspot.service.SpotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotControllerTest {

    @Mock
    private SpotService service;

    @InjectMocks
    private SpotController controller;

    private SpotResponseDTO responseDTO;
    private SpotRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        responseDTO = new SpotResponseDTO();

        requestDTO = new SpotRequestDTO();
        requestDTO.setLotId(10L);
        requestDTO.setSpotNumber("A1-01");
        requestDTO.setFloor(1);
        requestDTO.setSpotType(SpotType.COMPACT);
        requestDTO.setVehicleType(VehicleType.FOUR_WHEELER);
        requestDTO.setPricePerHour(50.0);
    }

    // ── GET /api/spots/{spotId} ───────────────────────────────────────────────

    @Test
    void shouldGetSpotById() {
        when(service.getSpotById(1L)).thenReturn(responseDTO);

        var response = controller.getById(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).getSpotById(1L);
    }

    // ── GET /api/spots/lot/{lotId} ────────────────────────────────────────────

    @Test
    void shouldGetSpotsByLot() {
        when(service.getSpotsByLot(10L)).thenReturn(List.of(responseDTO));

        var response = controller.getByLot(10L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(service).getSpotsByLot(10L);
    }

    // ── GET /api/spots/lot/{lotId}/available ──────────────────────────────────

    @Test
    void shouldGetAvailableSpots() {
        when(service.getAvailableSpots(10L)).thenReturn(List.of(responseDTO));

        var response = controller.getAvailable(10L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(service).getAvailableSpots(10L);
    }

    // ── GET /api/spots/lot/{lotId}/floor/{floor} ──────────────────────────────

    @Test
    void shouldGetSpotsByFloor() {
        when(service.getSpotsByFloor(10L, 1)).thenReturn(List.of(responseDTO));

        var response = controller.getByFloor(10L, 1);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(service).getSpotsByFloor(10L, 1);
    }

    // ── GET /api/spots/lot/{lotId}/type/{spotType} ────────────────────────────

    @Test
    void shouldGetSpotsByType() {
        when(service.getSpotsByType(10L, SpotType.COMPACT)).thenReturn(List.of(responseDTO));

        var response = controller.getByType(10L, SpotType.COMPACT);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(service).getSpotsByType(10L, SpotType.COMPACT);
    }

    // ── GET /api/spots/lot/{lotId}/vehicle/{vehicleType} ──────────────────────

    @Test
    void shouldGetSpotsByVehicleType() {
        when(service.getSpotsByVehicleType(10L, VehicleType.FOUR_WHEELER))
                .thenReturn(List.of(responseDTO));

        var response = controller.getByVehicleType(10L, VehicleType.FOUR_WHEELER);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(service).getSpotsByVehicleType(10L, VehicleType.FOUR_WHEELER);
    }

    // ── GET /api/spots/lot/{lotId}/ev ─────────────────────────────────────────

    @Test
    void shouldGetEVSpots() {
        when(service.getEVSpots(10L)).thenReturn(List.of(responseDTO));

        var response = controller.getEVSpots(10L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(service).getEVSpots(10L);
    }

    // ── GET /api/spots/lot/{lotId}/handicapped ────────────────────────────────

    @Test
    void shouldGetHandicappedSpots() {
        when(service.getHandicappedSpots(10L)).thenReturn(List.of(responseDTO));

        var response = controller.getHandicappedSpots(10L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(service).getHandicappedSpots(10L);
    }

    // ── GET /api/spots/lot/{lotId}/count ──────────────────────────────────────

    @Test
    void shouldGetAvailableCount() {
        when(service.countAvailableSpots(10L)).thenReturn(8);

        var response = controller.getAvailableCount(10L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(8, response.getBody());
        verify(service).countAvailableSpots(10L);
    }

    // ── POST /api/spots ───────────────────────────────────────────────────────

    @Test
    void shouldAddSpotAndReturn201() {
        when(service.addSpot(requestDTO)).thenReturn(responseDTO);

        var response = controller.addSpot(requestDTO);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).addSpot(requestDTO);
    }

    // ── POST /api/spots/bulk ──────────────────────────────────────────────────

    @Test
    void shouldAddBulkSpotsAndReturn201() {
        BulkSpotRequestDTO dto = new BulkSpotRequestDTO();
        dto.setLotId(10L);
        dto.setPrefix("A");
        dto.setFloor(1);
        dto.setCount(3);
        dto.setSpotType(SpotType.COMPACT);
        dto.setVehicleType(VehicleType.FOUR_WHEELER);
        dto.setPricePerHour(50.0);

        when(service.addBulkSpots(dto)).thenReturn(List.of(responseDTO, responseDTO, responseDTO));

        var response = controller.addBulk(dto);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(3, response.getBody().size());
        verify(service).addBulkSpots(dto);
    }

    // ── PUT /api/spots/{spotId} ───────────────────────────────────────────────

    @Test
    void shouldUpdateSpot() {
        when(service.updateSpot(1L, requestDTO)).thenReturn(responseDTO);

        var response = controller.updateSpot(1L, requestDTO);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).updateSpot(1L, requestDTO);
    }

    // ── DELETE /api/spots/{spotId} ────────────────────────────────────────────

    @Test
    void shouldDeleteSpot() {
        doNothing().when(service).deleteSpot(1L);

        var response = controller.deleteSpot(1L);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        verify(service).deleteSpot(1L);
    }

    // ── PUT /api/spots/{spotId}/maintenance ───────────────────────────────────

    @Test
    void shouldSetMaintenance() {
        when(service.setMaintenance(1L)).thenReturn(responseDTO);

        var response = controller.setMaintenance(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).setMaintenance(1L);
    }

    // ── PUT /api/spots/{spotId}/reserve ──────────────────────────────────────

    @Test
    void shouldReserveSpot() {
        when(service.reserveSpot(1L)).thenReturn(responseDTO);

        var response = controller.reserve(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).reserveSpot(1L);
    }

    // ── PUT /api/spots/{spotId}/occupy ────────────────────────────────────────

    @Test
    void shouldOccupySpot() {
        when(service.occupySpot(1L)).thenReturn(responseDTO);

        var response = controller.occupy(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).occupySpot(1L);
    }

    // ── PUT /api/spots/{spotId}/release ───────────────────────────────────────

    @Test
    void shouldReleaseSpot() {
        when(service.releaseSpot(1L)).thenReturn(responseDTO);

        var response = controller.release(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).releaseSpot(1L);
    }
}
