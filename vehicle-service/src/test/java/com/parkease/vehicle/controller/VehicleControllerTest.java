package com.parkease.vehicle.controller;

import com.parkease.vehicle.dto.request.VehicleRequestDTO;
import com.parkease.vehicle.dto.response.ApiResponse;
import com.parkease.vehicle.dto.response.VehicleResponseDTO;
import com.parkease.vehicle.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleControllerTest {

    @Mock
    private VehicleService service;

    @Mock
    private Authentication auth;

    @InjectMocks
    private VehicleController controller;

    private VehicleRequestDTO requestDTO;
    private VehicleResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new VehicleRequestDTO();
        requestDTO.setLicensePlate("KA-01-HH-1234");
        requestDTO.setEV(false);

        responseDTO = VehicleResponseDTO.builder()
                .vehicleId(1L)
                .licensePlate("KA-01-HH-1234")
                .isEV(false)
                .isActive(true)
                .build();
    }

    @Test
    void shouldRegisterVehicle() {
        when(auth.getPrincipal()).thenReturn("driver@test.com");
        when(service.registerVehicle(any(VehicleRequestDTO.class), eq("driver@test.com")))
                .thenReturn(responseDTO);

        ResponseEntity<VehicleResponseDTO> response = controller.register(requestDTO, auth);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(responseDTO, response.getBody());
        verify(service).registerVehicle(requestDTO, "driver@test.com");
    }

    @Test
    void shouldGetMyVehicles() {
        when(auth.getPrincipal()).thenReturn("driver@test.com");
        when(service.getMyVehicles("driver@test.com")).thenReturn(List.of(responseDTO));

        ResponseEntity<List<VehicleResponseDTO>> response = controller.getMyVehicles(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(service).getMyVehicles("driver@test.com");
    }

    @Test
    void shouldGetById() {
        when(service.getVehicleById(1L)).thenReturn(responseDTO);

        ResponseEntity<VehicleResponseDTO> response = controller.getById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDTO, response.getBody());
        verify(service).getVehicleById(1L);
    }

    @Test
    void shouldGetByPlate() {
        when(service.getByLicensePlate("KA-01-HH-1234")).thenReturn(responseDTO);

        ResponseEntity<VehicleResponseDTO> response = controller.getByPlate("KA-01-HH-1234");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDTO, response.getBody());
        verify(service).getByLicensePlate("KA-01-HH-1234");
    }

    @Test
    void shouldUpdateVehicle() {
        when(auth.getPrincipal()).thenReturn("driver@test.com");
        when(service.updateVehicle(eq(1L), any(VehicleRequestDTO.class), eq("driver@test.com")))
                .thenReturn(responseDTO);

        ResponseEntity<VehicleResponseDTO> response = controller.update(1L, requestDTO, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDTO, response.getBody());
        verify(service).updateVehicle(1L, requestDTO, "driver@test.com");
    }

    @Test
    void shouldDeleteVehicle() {
        when(auth.getPrincipal()).thenReturn("driver@test.com");

        ResponseEntity<ApiResponse> response = controller.delete(1L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).deleteVehicle(1L, "driver@test.com");
    }

    @Test
    void shouldDeactivateVehicle() {
        when(auth.getPrincipal()).thenReturn("driver@test.com");

        ResponseEntity<ApiResponse> response = controller.deactivate(1L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).deactivateVehicle(1L, "driver@test.com");
    }
}
