package com.parkease.booking.controller;

import com.parkease.booking.dto.request.CreateBookingRequest;
import com.parkease.booking.dto.request.ExtendBookingRequest;
import com.parkease.booking.dto.response.*;
import com.parkease.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

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
class BookingControllerTest {

    @Mock
    private BookingService service;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BookingController controller;

    private final String USER_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
    }

    private void mockAuth() {
        when(authentication.getPrincipal()).thenReturn(USER_EMAIL);
    }

    @Test
    void testGetAvailableForPreBooking() {
        List<Map<String, Object>> mockResp = List.of(Map.of("spotId", 1L));
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusHours(2);
        when(service.getAvailableSpotsForPreBooking(1L, start, end)).thenReturn(mockResp);

        ResponseEntity<List<Map<String, Object>>> response = controller.getAvailableForPreBooking(1L, start, end);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetDriveInView() {
        List<DriveInSpotDTO> mockResp = List.of(new DriveInSpotDTO());
        when(service.getDriveInSpotView(1L)).thenReturn(mockResp);

        ResponseEntity<List<DriveInSpotDTO>> response = controller.getDriveInView(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testCreate() {
        mockAuth();
        CreateBookingRequest req = new CreateBookingRequest();
        req.setSpotId(1L);
        BookingResponseDTO mockResp = BookingResponseDTO.builder().bookingId(1L).build();
        when(service.createBooking(req, USER_EMAIL)).thenReturn(mockResp);

        ResponseEntity<BookingResponseDTO> response = controller.create(req, authentication);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(1L, response.getBody().getBookingId());
    }

    @Test
    void testCheckIn() {
        mockAuth();
        BookingResponseDTO mockResp = BookingResponseDTO.builder().bookingId(1L).build();
        when(service.checkIn(1L, USER_EMAIL)).thenReturn(mockResp);

        ResponseEntity<BookingResponseDTO> response = controller.checkIn(1L, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1L, response.getBody().getBookingId());
    }

    @Test
    void testCheckOut() {
        mockAuth();
        BookingResponseDTO mockResp = BookingResponseDTO.builder().bookingId(1L).build();
        when(service.checkOut(1L, USER_EMAIL)).thenReturn(mockResp);

        ResponseEntity<BookingResponseDTO> response = controller.checkOut(1L, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1L, response.getBody().getBookingId());
    }

    @Test
    void testCancel() {
        mockAuth();
        BookingResponseDTO mockResp = BookingResponseDTO.builder().bookingId(1L).build();
        when(service.cancelBooking(1L, USER_EMAIL)).thenReturn(mockResp);

        ResponseEntity<BookingResponseDTO> response = controller.cancel(1L, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1L, response.getBody().getBookingId());
    }

    @Test
    void testExtend() {
        mockAuth();
        ExtendBookingRequest req = new ExtendBookingRequest();
        BookingResponseDTO mockResp = BookingResponseDTO.builder().bookingId(1L).build();
        when(service.extendBooking(1L, req, USER_EMAIL)).thenReturn(mockResp);

        ResponseEntity<BookingResponseDTO> response = controller.extend(1L, req, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1L, response.getBody().getBookingId());
    }

    @Test
    void testGetMyBookings() {
        mockAuth();
        List<BookingResponseDTO> mockResp = List.of(BookingResponseDTO.builder().bookingId(1L).build());
        when(service.getMyBookings(USER_EMAIL)).thenReturn(mockResp);

        ResponseEntity<List<BookingResponseDTO>> response = controller.getMyBookings(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetMyActive() {
        mockAuth();
        List<BookingResponseDTO> mockResp = List.of(BookingResponseDTO.builder().bookingId(1L).build());
        when(service.getActiveBookings(USER_EMAIL)).thenReturn(mockResp);

        ResponseEntity<List<BookingResponseDTO>> response = controller.getMyActive(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetById() {
        BookingResponseDTO mockResp = BookingResponseDTO.builder().bookingId(1L).build();
        when(service.getBookingById(1L)).thenReturn(mockResp);

        ResponseEntity<BookingResponseDTO> response = controller.getById(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1L, response.getBody().getBookingId());
    }

    @Test
    void testGetFare() {
        when(service.calculateFare(1L)).thenReturn(100.0);

        ResponseEntity<Double> response = controller.getFare(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(100.0, response.getBody());
    }

    @Test
    void testGetManagerDashboard() {
        ManagerDashboardDTO mockResp = ManagerDashboardDTO.builder().build();
        when(service.getManagerDashboard(1L)).thenReturn(mockResp);

        ResponseEntity<ManagerDashboardDTO> response = controller.getManagerDashboard(1L);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void testGetManagerActive() {
        List<BookingResponseDTO> mockResp = List.of(BookingResponseDTO.builder().bookingId(1L).build());
        when(service.getActiveBookingsByLot(1L)).thenReturn(mockResp);

        ResponseEntity<List<BookingResponseDTO>> response = controller.getManagerActive(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetManagerUpcoming() {
        List<BookingResponseDTO> mockResp = List.of(BookingResponseDTO.builder().bookingId(1L).build());
        when(service.getUpcomingBookingsByLot(1L)).thenReturn(mockResp);

        ResponseEntity<List<BookingResponseDTO>> response = controller.getManagerUpcoming(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetByLot() {
        List<BookingResponseDTO> mockResp = List.of(BookingResponseDTO.builder().bookingId(1L).build());
        when(service.getBookingsByLot(1L)).thenReturn(mockResp);

        ResponseEntity<List<BookingResponseDTO>> response = controller.getByLot(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testManualCheckOut() {
        mockAuth();
        BookingResponseDTO mockResp = BookingResponseDTO.builder().bookingId(1L).build();
        when(service.manualCheckOut(1L, USER_EMAIL)).thenReturn(mockResp);

        ResponseEntity<BookingResponseDTO> response = controller.manualCheckOut(1L, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1L, response.getBody().getBookingId());
    }

    @Test
    void testGetAll() {
        List<BookingResponseDTO> mockResp = List.of(BookingResponseDTO.builder().bookingId(1L).build());
        when(service.getAllBookings()).thenReturn(mockResp);

        ResponseEntity<List<BookingResponseDTO>> response = controller.getAll();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetDistinctLotIds() {
        List<Long> mockResp = List.of(1L, 2L);
        when(service.getDistinctLotIds()).thenReturn(mockResp);

        ResponseEntity<List<Long>> response = controller.getDistinctLotIds();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, response.getBody().size());
    }
}
