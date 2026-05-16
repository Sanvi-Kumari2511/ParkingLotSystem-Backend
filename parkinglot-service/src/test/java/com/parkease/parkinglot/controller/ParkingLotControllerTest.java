package com.parkease.parkinglot.controller;

import com.parkease.parkinglot.dto.request.ParkingLotRequestDTO;
import com.parkease.parkinglot.dto.response.ApiResponse;
import com.parkease.parkinglot.dto.response.ParkingLotResponseDTO;
import com.parkease.parkinglot.service.ParkingLotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingLotControllerTest {

    @Mock
    private ParkingLotService service;

    @Mock
    private Authentication auth;

    @InjectMocks
    private ParkingLotController controller;

    private ParkingLotResponseDTO responseDTO;
    private ParkingLotRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        responseDTO = new ParkingLotResponseDTO();

        requestDTO = new ParkingLotRequestDTO();
        requestDTO.setName("City Mall Parking");
        requestDTO.setAddress("MP Nagar");
        requestDTO.setCity("Bhopal");
        requestDTO.setLatitude(23.2599);
        requestDTO.setLongitude(77.4126);
        requestDTO.setTotalSpots(100);
        requestDTO.setOpenTime(LocalTime.of(8, 0));
        requestDTO.setCloseTime(LocalTime.of(22, 0));
    }

    // ── GET /api/lots ─────────────────────────────────────────────────────────

    @Test
    void shouldGetAllOpenLots() {
        when(service.getOpenLots()).thenReturn(List.of(responseDTO));

        var response = controller.getAllOpenLots();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(service).getOpenLots();
    }

    // ── GET /api/lots/{id} ────────────────────────────────────────────────────

    @Test
    void shouldGetLotById() {
        when(service.getLotById(1L)).thenReturn(responseDTO);

        var response = controller.getLotById(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).getLotById(1L);
    }

    // ── GET /api/lots/city/{city} ─────────────────────────────────────────────

    @Test
    void shouldGetLotsByCity() {
        when(service.getByCity("Bhopal")).thenReturn(List.of(responseDTO));

        var response = controller.getLotsByCity("Bhopal");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(service).getByCity("Bhopal");
    }

    // ── GET /api/lots/nearby ──────────────────────────────────────────────────

    @Test
    void shouldGetNearbyLots() {
        when(service.getNearbyLots(23.2599, 77.4126, 5.0))
                .thenReturn(List.of(responseDTO));

        var response = controller.getNearbyLots(23.2599, 77.4126, 5.0);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(service).getNearbyLots(23.2599, 77.4126, 5.0);
    }

    // ── POST /api/lots ────────────────────────────────────────────────────────

    @Test
    void shouldCreateLotAndReturn201() {
        when(auth.getPrincipal()).thenReturn("manager@test.com");
        when(service.createLot(requestDTO, "manager@test.com")).thenReturn(responseDTO);

        var response = controller.createLot(requestDTO, auth);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).createLot(requestDTO, "manager@test.com");
    }

    // ── PUT /api/lots/{id} ────────────────────────────────────────────────────

    @Test
    void shouldUpdateLot() {
        when(auth.getPrincipal()).thenReturn("manager@test.com");
        when(service.updateLot(1L, requestDTO, "manager@test.com")).thenReturn(responseDTO);

        var response = controller.updateLot(1L, requestDTO, auth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).updateLot(1L, requestDTO, "manager@test.com");
    }

    // ── DELETE /api/lots/{id} ─────────────────────────────────────────────────

    @Test
    void shouldDeleteLot() {
        when(auth.getPrincipal()).thenReturn("manager@test.com");
        doNothing().when(service).deleteLot(1L, "manager@test.com");

        var response = controller.deleteLot(1L, auth);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        verify(service).deleteLot(1L, "manager@test.com");
    }

    // ── PUT /api/lots/{id}/toggle ─────────────────────────────────────────────

    @Test
    void shouldToggleLotOpen() {
        when(auth.getPrincipal()).thenReturn("manager@test.com");
        when(service.toggleOpen(1L, "manager@test.com")).thenReturn(responseDTO);

        var response = controller.toggleOpen(1L, auth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).toggleOpen(1L, "manager@test.com");
    }

    // ── GET /api/lots/my-lots ─────────────────────────────────────────────────

    @Test
    void shouldGetMyLots() {
        when(auth.getPrincipal()).thenReturn("manager@test.com");
        when(service.getLotsByManager("manager@test.com")).thenReturn(List.of(responseDTO));

        var response = controller.getMyLots(auth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(service).getLotsByManager("manager@test.com");
    }

    // ── GET /api/lots/admin/pending ───────────────────────────────────────────

    @Test
    void shouldGetPendingLots() {
        when(service.getPendingApprovalLots()).thenReturn(List.of(responseDTO));

        var response = controller.getPendingLots();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(service).getPendingApprovalLots();
    }

    // ── GET /api/lots/admin/all ───────────────────────────────────────────────

    @Test
    void shouldGetAllApprovedLots() {
        when(service.getAllApprovedLots()).thenReturn(List.of(responseDTO));

        var response = controller.getAllApprovedLots();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(service).getAllApprovedLots();
    }

    // ── PUT /api/lots/admin/{id}/approve ─────────────────────────────────────

    @Test
    void shouldApproveLot() {
        when(auth.getPrincipal()).thenReturn("admin@test.com");
        when(service.approveLot(1L, "Looks great!", "admin@test.com")).thenReturn(responseDTO);

        var response = controller.approveLot(1L, "Looks great!", auth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).approveLot(1L, "Looks great!", "admin@test.com");
    }

    // ── PUT /api/lots/admin/{id}/reject ──────────────────────────────────────

    @Test
    void shouldRejectLot() {
        when(auth.getPrincipal()).thenReturn("admin@test.com");
        when(service.rejectLot(1L, "Incomplete details", "admin@test.com")).thenReturn(responseDTO);

        var response = controller.rejectLot(1L, "Incomplete details", auth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).rejectLot(1L, "Incomplete details", "admin@test.com");
    }

    // ── PUT /api/lots/{id}/decrement ──────────────────────────────────────────

    @Test
    void shouldDecrementSpot() {
        doNothing().when(service).decrementSpot(1L);

        var response = controller.decrementSpot(1L);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        verify(service).decrementSpot(1L);
    }

    // ── PUT /api/lots/{id}/increment ──────────────────────────────────────────

    @Test
    void shouldIncrementSpot() {
        doNothing().when(service).incrementSpot(1L);

        var response = controller.incrementSpot(1L);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        verify(service).incrementSpot(1L);
    }
}
