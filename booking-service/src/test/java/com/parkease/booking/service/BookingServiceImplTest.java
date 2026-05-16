package com.parkease.booking.service;

import java.util.List;
import com.parkease.booking.client.AnalyticsServiceClient;
import com.parkease.booking.client.LotServiceClient;
import com.parkease.booking.client.SpotServiceClient;
import com.parkease.booking.client.VehicleServiceClient;
import com.parkease.booking.dto.request.CreateBookingRequest;
import com.parkease.booking.dto.request.ExtendBookingRequest;
import com.parkease.booking.dto.response.BookingResponseDTO;
import com.parkease.booking.entity.*;
import com.parkease.booking.exception.BookingException;
import com.parkease.booking.mapper.BookingMapper;
import com.parkease.booking.messaging.NotificationPublisher;
import com.parkease.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository repo;

    @Mock
    private BookingMapper mapper;

    @Mock
    private SpotServiceClient spotServiceClient;

    @Mock
    private LotServiceClient lotServiceClient;

    @Mock
    private VehicleServiceClient vehicleServiceClient;

    @Mock
    private AnalyticsServiceClient analyticsClient;

    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private BookingServiceImpl service;

    private Booking booking;
    private BookingResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "graceMinutes", 15);

        booking = Booking.builder()
                .bookingId(1L)
                .driverEmail("sanvi@test.com")
                .lotId(10L)
                .spotId(101L)
                .vehiclePlate("MP04AB1234")
                .bookingType(BookingType.PRE_BOOKING)
                .status(BookingStatus.RESERVED)
                .startTime(LocalDateTime.now().plusMinutes(5))
                .endTime(LocalDateTime.now().plusHours(2))
                .pricePerHour(50.0)
                .build();

        responseDTO = new BookingResponseDTO();
    }

    @Test
    void shouldCreateBookingSuccessfully() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setLotId(10L);
        req.setSpotId(101L);
        req.setVehiclePlate("mp04ab1234");
        req.setBookingType(BookingType.PRE_BOOKING);
        req.setStartTime(LocalDateTime.now().plusMinutes(5));
        req.setEndTime(LocalDateTime.now().plusHours(2));

        when(repo.isSpotBookedInWindow(anyLong(), any(), any())).thenReturn(false);
        when(spotServiceClient.getSpotById(anyLong())).thenReturn(Map.of("pricePerHour", 50.0, "vehicleType", "FOUR_WHEELER"));
        when(vehicleServiceClient.getVehicleByPlate(anyString())).thenReturn(Map.of("vehicleType", "FOUR_WHEELER", "ownerEmail", "sanvi@test.com"));
        when(repo.save(any(Booking.class))).thenReturn(booking);
        when(mapper.toDTO(any(Booking.class))).thenReturn(responseDTO);

        BookingResponseDTO result = service.createBooking(req, "sanvi@test.com");

        assertNotNull(result);
        verify(repo).save(any(Booking.class));
        verify(spotServiceClient).reserveSpot(anyLong());
        verify(lotServiceClient).decrementAvailable(anyLong());
    }

    @Test
    void shouldThrowWhenSpotAlreadyBooked() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setSpotId(101L);
        req.setVehiclePlate("MP04AB1234");
        req.setStartTime(LocalDateTime.now().plusMinutes(5));
        req.setEndTime(LocalDateTime.now().plusHours(1));

        when(spotServiceClient.getSpotById(anyLong())).thenReturn(Map.of("vehicleType", "FOUR_WHEELER"));
        when(vehicleServiceClient.getVehicleByPlate(anyString())).thenReturn(Map.of("vehicleType", "FOUR_WHEELER", "ownerEmail", "sanvi@test.com"));

        when(repo.isSpotBookedInWindow(anyLong(), any(), any())).thenReturn(true);

        assertThrows(BookingException.class,
                () -> service.createBooking(req, "sanvi@test.com"));
    }

    @Test
    void shouldCheckInSuccessfully() {
        booking.setStartTime(LocalDateTime.now().minusMinutes(5));
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);

        BookingResponseDTO result = service.checkIn(1L, "sanvi@test.com");

        assertNotNull(result);
        assertEquals(BookingStatus.ACTIVE, booking.getStatus());
        verify(spotServiceClient).occupySpot(anyLong());
    }

    @Test
    void shouldCheckOutSuccessfully() {
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setCheckInTime(LocalDateTime.now().minusHours(2));

        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);

        BookingResponseDTO result = service.checkOut(1L, "sanvi@test.com");

        assertNotNull(result);
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
        assertTrue(booking.getTotalAmount() > 0);
        verify(spotServiceClient).releaseSpot(anyLong());
        verify(lotServiceClient).incrementAvailable(anyLong());
    }

    @Test
    void shouldManualCheckOutSuccessfully() {
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setCheckInTime(LocalDateTime.now().minusHours(2));

        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);

        BookingResponseDTO result = service.manualCheckOut(1L, "admin@test.com");

        assertNotNull(result);
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
        verify(analyticsClient).logAuditAction(anyMap(), anyString());
    }

    @Test
    void shouldCancelBookingSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);

        BookingResponseDTO result = service.cancelBooking(1L, "sanvi@test.com");

        assertNotNull(result);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(spotServiceClient).releaseSpot(anyLong());
        verify(lotServiceClient).incrementAvailable(anyLong());
    }

    @Test
    void shouldExtendBookingSuccessfully() {
        ExtendBookingRequest req = new ExtendBookingRequest();
        req.setNewEndTime(booking.getEndTime().plusHours(2));

        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(repo.isSpotBookedInWindow(anyLong(), any(), any())).thenReturn(false);
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);

        BookingResponseDTO result = service.extendBooking(1L, req, "sanvi@test.com");

        assertNotNull(result);
        assertEquals(req.getNewEndTime(), booking.getEndTime());
    }

    @Test
    void shouldCalculateFareSuccessfully() {
        booking.setCheckInTime(LocalDateTime.now().minusHours(2));
        booking.setCheckOutTime(LocalDateTime.now());

        when(repo.findById(1L)).thenReturn(Optional.of(booking));

        double fare = service.calculateFare(1L);

        assertEquals(100.0, fare);
    }

    @Test
    void shouldReturnAvailableSpotsForPreBooking() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(3);

        List<Map<String, Object>> mockSpots = java.util.Arrays.asList(
                Map.of("spotId", 101L, "vehicleType", "FOUR_WHEELER", "pricePerHour", 50.0),
                Map.of("spotId", 102L, "vehicleType", "TWO_WHEELER", "pricePerHour", 20.0)
        );

        when(spotServiceClient.getSpotsByLot(10L)).thenReturn(mockSpots);
        when(repo.findBookedSpotIdsInWindow(10L, start, end)).thenReturn(java.util.Arrays.asList(101L));

        java.util.List<Map<String, Object>> result = service.getAvailableSpotsForPreBooking(10L, start, end);

        assertEquals(1, result.size());
        assertEquals(102L, result.get(0).get("spotId"));
    }

    @Test
    void shouldReturnDriveInSpotView() {
        List<Map<String, Object>> mockSpots = java.util.Arrays.asList(
                Map.of("spotId", 101L, "vehicleType", "FOUR_WHEELER")
        );

        when(spotServiceClient.getSpotsByLot(10L)).thenReturn(mockSpots);
        when(repo.findActiveOrReservedBookingsForSpot(101L)).thenReturn(java.util.Collections.emptyList());

        java.util.List<com.parkease.booking.dto.response.DriveInSpotDTO> result = service.getDriveInSpotView(10L);

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getSpotId());
        assertEquals("FREE", result.get(0).getStatus());
    }

    @Test
    void shouldReturnManagerDashboard() {
        Booking active = new Booking();
        active.setStatus(BookingStatus.ACTIVE);
        Booking upcoming = new Booking();
        upcoming.setStatus(BookingStatus.RESERVED);

        when(repo.findActiveBookingsByLot(10L)).thenReturn(java.util.Collections.singletonList(active));
        when(repo.findUpcomingBookingsByLot(eq(10L), any(LocalDateTime.class))).thenReturn(java.util.Collections.singletonList(upcoming));
        when(mapper.toDTO(active)).thenReturn(new BookingResponseDTO());
        when(mapper.toDTO(upcoming)).thenReturn(new BookingResponseDTO());

        com.parkease.booking.dto.response.ManagerDashboardDTO result = service.getManagerDashboard(10L);

        assertEquals(1, result.getActiveBookings().size());
        assertEquals(1, result.getUpcomingBookings().size());
    }

    @Test
    void shouldReturnActiveBookingsByLot() {
        when(repo.findActiveBookingsByLot(10L)).thenReturn(java.util.Collections.singletonList(booking));
        when(mapper.toDTO(booking)).thenReturn(responseDTO);

        java.util.List<BookingResponseDTO> result = service.getActiveBookingsByLot(10L);

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnUpcomingBookingsByLot() {
        when(repo.findUpcomingBookingsByLot(eq(10L), any(LocalDateTime.class)))
                .thenReturn(java.util.Collections.singletonList(booking));
        when(mapper.toDTO(booking)).thenReturn(responseDTO);

        java.util.List<BookingResponseDTO> result = service.getUpcomingBookingsByLot(10L);

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnBookingsByLot() {
        when(repo.findByLotIdOrderByCreatedAtDesc(10L)).thenReturn(java.util.Collections.singletonList(booking));
        when(mapper.toDTO(booking)).thenReturn(responseDTO);

        java.util.List<BookingResponseDTO> result = service.getBookingsByLot(10L);

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnAllBookings() {
        when(repo.findAll()).thenReturn(java.util.Collections.singletonList(booking));
        when(mapper.toDTO(booking)).thenReturn(responseDTO);

        java.util.List<BookingResponseDTO> result = service.getAllBookings();

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnDistinctLotIds() {
        when(repo.findDistinctLotIds()).thenReturn(java.util.Arrays.asList(1L, 2L));

        java.util.List<Long> result = service.getDistinctLotIds();

        assertEquals(2, result.size());
    }

    @Test
    void shouldReturnMyBookings() {
        when(repo.findByDriverEmailOrderByCreatedAtDesc("sanvi@test.com")).thenReturn(java.util.Collections.singletonList(booking));
        when(mapper.toDTO(booking)).thenReturn(responseDTO);

        java.util.List<BookingResponseDTO> result = service.getMyBookings("sanvi@test.com");

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnActiveBookings() {
        when(repo.findByDriverEmailAndStatus("sanvi@test.com", BookingStatus.ACTIVE))
                .thenReturn(java.util.Collections.singletonList(booking));
        when(mapper.toDTO(booking)).thenReturn(responseDTO);

        java.util.List<BookingResponseDTO> result = service.getActiveBookings("sanvi@test.com");

        assertEquals(1, result.size());
    }

    @Test
    void shouldThrowWhenCreateDriveInMissingEndTime() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setBookingType(BookingType.DRIVE_IN);
        req.setSpotId(101L);
        req.setVehiclePlate("MP04AB1234");
        
        when(spotServiceClient.getSpotById(anyLong())).thenReturn(Map.of("vehicleType", "FOUR_WHEELER"));
        when(vehicleServiceClient.getVehicleByPlate(anyString())).thenReturn(Map.of("vehicleType", "FOUR_WHEELER", "ownerEmail", "sanvi@test.com"));
        // missing end time

        assertThrows(IllegalArgumentException.class, () -> service.createBooking(req, "sanvi@test.com"));
    }

    @Test
    void shouldCreateDriveInBookingSuccessfully() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setLotId(10L);
        req.setSpotId(101L);
        req.setVehiclePlate("mp04ab1234");
        req.setBookingType(BookingType.DRIVE_IN);
        req.setEndTime(LocalDateTime.now().plusHours(2));

        when(repo.isSpotBookedInWindow(anyLong(), any(), any())).thenReturn(false);
        when(spotServiceClient.getSpotById(anyLong())).thenReturn(Map.of("pricePerHour", 50.0, "vehicleType", "FOUR_WHEELER"));
        when(vehicleServiceClient.getVehicleByPlate(anyString())).thenReturn(Map.of("vehicleType", "FOUR_WHEELER", "ownerEmail", "sanvi@test.com"));
        when(repo.save(any(Booking.class))).thenReturn(booking);
        when(mapper.toDTO(any(Booking.class))).thenReturn(responseDTO);

        BookingResponseDTO result = service.createBooking(req, "sanvi@test.com");

        assertNotNull(result);
        verify(repo).save(any(Booking.class));
    }

    @Test
    void shouldThrowWhenCheckInInvalidStatus() {
        booking.setStatus(BookingStatus.ACTIVE);
        when(repo.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(BookingException.class, () -> service.checkIn(1L, "sanvi@test.com"));
    }

    @Test
    void shouldThrowWhenCheckOutInvalidStatus() {
        booking.setStatus(BookingStatus.RESERVED);
        when(repo.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(BookingException.class, () -> service.checkOut(1L, "sanvi@test.com"));
    }
}