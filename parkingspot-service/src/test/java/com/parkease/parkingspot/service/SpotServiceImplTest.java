package com.parkease.parkingspot.service;

import com.parkease.parkingspot.dto.request.BulkSpotRequestDTO;
import com.parkease.parkingspot.dto.request.SpotRequestDTO;
import com.parkease.parkingspot.dto.response.SpotResponseDTO;
import com.parkease.parkingspot.entity.ParkingSpot;
import com.parkease.parkingspot.entity.SpotStatus;
import com.parkease.parkingspot.entity.SpotType;
import com.parkease.parkingspot.entity.VehicleType;
import com.parkease.parkingspot.exception.SpotNotAvailableException;
import com.parkease.parkingspot.mapper.SpotMapper;
import com.parkease.parkingspot.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotServiceImplTest {

    @Mock
    private SpotRepository repo;

    @Mock
    private SpotMapper mapper;

    @InjectMocks
    private SpotServiceImpl service;

    private ParkingSpot spot;
    private SpotRequestDTO requestDTO;
    private SpotResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        spot = ParkingSpot.builder()
                .spotId(1L)
                .lotId(10L)
                .spotNumber("A1-01")
                .floor(1)
                .spotType(SpotType.COMPACT)
                .vehicleType(VehicleType.FOUR_WHEELER)
                .status(SpotStatus.AVAILABLE)
                .pricePerHour(50.0)
                .build();

        requestDTO = new SpotRequestDTO();
        requestDTO.setLotId(10L);
        requestDTO.setSpotNumber("A1-01");
        requestDTO.setFloor(1);
        requestDTO.setSpotType(SpotType.COMPACT);
        requestDTO.setVehicleType(VehicleType.FOUR_WHEELER);
        requestDTO.setPricePerHour(50.0);

        responseDTO = new SpotResponseDTO();
    }

    @Test
    void shouldAddSpotSuccessfully() {
        when(repo.existsByLotIdAndSpotNumber(10L, "A1-01")).thenReturn(false);
        when(mapper.toEntity(requestDTO)).thenReturn(spot);
        when(repo.save(spot)).thenReturn(spot);
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        SpotResponseDTO result = service.addSpot(requestDTO);

        assertNotNull(result);
        verify(repo).save(spot);
    }

    @Test
    void shouldThrowWhenDuplicateSpotExists() {
        when(repo.existsByLotIdAndSpotNumber(10L, "A1-01")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.addSpot(requestDTO));
    }

    @Test
    void shouldBulkCreateSpotsSuccessfully() {
        BulkSpotRequestDTO dto = new BulkSpotRequestDTO();
        dto.setLotId(10L);
        dto.setPrefix("A");
        dto.setFloor(1);
        dto.setCount(2);
        dto.setSpotType(SpotType.COMPACT);
        dto.setVehicleType(VehicleType.FOUR_WHEELER);
        dto.setPricePerHour(50.0);

        when(repo.existsByLotIdAndSpotNumber(anyLong(), anyString()))
                .thenReturn(false);
        when(repo.saveAll(anyList()))
                .thenReturn(List.of(
                        spot,
                        ParkingSpot.builder()
                                .spotId(2L)
                                .lotId(10L)
                                .spotNumber("A1-02")
                                .status(SpotStatus.AVAILABLE)
                                .build()
                ));
        when(mapper.toDTO(any())).thenReturn(responseDTO);

        List<SpotResponseDTO> result = service.addBulkSpots(dto);

        assertEquals(2, result.size());
        verify(repo).saveAll(anyList());
    }

    @Test
    void shouldDeleteSpotSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(spot));

        service.deleteSpot(1L);

        verify(repo).delete(spot);
    }

    @Test
    void shouldThrowWhenDeletingReservedSpot() {
        spot.setStatus(SpotStatus.RESERVED);
        when(repo.findById(1L)).thenReturn(Optional.of(spot));

        assertThrows(SpotNotAvailableException.class,
                () -> service.deleteSpot(1L));
    }

    @Test
    void shouldReserveSpotSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(spot));
        when(repo.save(spot)).thenReturn(spot);
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        SpotResponseDTO result = service.reserveSpot(1L);

        assertNotNull(result);
        assertEquals(SpotStatus.RESERVED, spot.getStatus());
    }

    @Test
    void shouldOccupySpotSuccessfully() {
        spot.setStatus(SpotStatus.RESERVED);

        when(repo.findById(1L)).thenReturn(Optional.of(spot));
        when(repo.save(spot)).thenReturn(spot);
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        SpotResponseDTO result = service.occupySpot(1L);

        assertNotNull(result);
        assertEquals(SpotStatus.OCCUPIED, spot.getStatus());
    }

    @Test
    void shouldReleaseSpotSuccessfully() {
        spot.setStatus(SpotStatus.OCCUPIED);

        when(repo.findById(1L)).thenReturn(Optional.of(spot));
        when(repo.save(spot)).thenReturn(spot);
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        SpotResponseDTO result = service.releaseSpot(1L);

        assertNotNull(result);
        assertEquals(SpotStatus.AVAILABLE, spot.getStatus());
    }

    @Test
    void shouldSetMaintenanceSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(spot));
        when(repo.save(spot)).thenReturn(spot);
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        SpotResponseDTO result = service.setMaintenance(1L);

        assertNotNull(result);
        assertEquals(SpotStatus.MAINTENANCE, spot.getStatus());
    }

    @Test
    void shouldCountAvailableSpotsSuccessfully() {
        when(repo.countByLotIdAndStatus(10L, SpotStatus.AVAILABLE))
                .thenReturn(5);

        int result = service.countAvailableSpots(10L);

        assertEquals(5, result);
    }

    // ── Edge-case / additional branch tests ───────────────────────────────────

    @Test
    void shouldThrowWhenSpotNotFound() {
        when(repo.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThrows(com.parkease.parkingspot.exception.ResourceNotFoundException.class,
                () -> service.getSpotById(99L));
    }

    @Test
    void shouldThrowWhenDeletingOccupiedSpot() {
        spot.setStatus(SpotStatus.OCCUPIED);
        when(repo.findById(1L)).thenReturn(java.util.Optional.of(spot));

        assertThrows(SpotNotAvailableException.class, () -> service.deleteSpot(1L));
        verify(repo, never()).delete(any());
    }

    @Test
    void shouldThrowWhenReservingNonAvailableSpot() {
        spot.setStatus(SpotStatus.RESERVED);
        when(repo.findById(1L)).thenReturn(java.util.Optional.of(spot));

        assertThrows(SpotNotAvailableException.class, () -> service.reserveSpot(1L));
    }

    @Test
    void shouldThrowWhenOccupyingAlreadyOccupiedSpot() {
        spot.setStatus(SpotStatus.OCCUPIED);
        when(repo.findById(1L)).thenReturn(java.util.Optional.of(spot));

        assertThrows(SpotNotAvailableException.class, () -> service.occupySpot(1L));
    }

    @Test
    void shouldReturnExistingDtoWhenReleasingAlreadyAvailableSpot() {
        // spot.status is already AVAILABLE from setUp()
        when(repo.findById(1L)).thenReturn(java.util.Optional.of(spot));
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        SpotResponseDTO result = service.releaseSpot(1L);

        assertNotNull(result);
        verify(repo, never()).save(any()); // should NOT save if already AVAILABLE
    }

    @Test
    void shouldGetSpotsByLotSuccessfully() {
        when(repo.findByLotId(10L)).thenReturn(List.of(spot));
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        List<SpotResponseDTO> result = service.getSpotsByLot(10L);

        assertEquals(1, result.size());
    }

    @Test
    void shouldGetAvailableSpotsSuccessfully() {
        when(repo.findByLotIdAndStatus(10L, SpotStatus.AVAILABLE)).thenReturn(List.of(spot));
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        List<SpotResponseDTO> result = service.getAvailableSpots(10L);

        assertEquals(1, result.size());
    }

    @Test
    void shouldGetSpotsByTypeSuccessfully() {
        when(repo.findByLotIdAndSpotType(10L, SpotType.COMPACT)).thenReturn(List.of(spot));
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        List<SpotResponseDTO> result = service.getSpotsByType(10L, SpotType.COMPACT);

        assertEquals(1, result.size());
    }

    @Test
    void shouldGetSpotsByVehicleTypeSuccessfully() {
        when(repo.findByLotIdAndVehicleType(10L, VehicleType.FOUR_WHEELER)).thenReturn(List.of(spot));
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        List<SpotResponseDTO> result = service.getSpotsByVehicleType(10L, VehicleType.FOUR_WHEELER);

        assertEquals(1, result.size());
    }

    @Test
    void shouldGetSpotsByFloorSuccessfully() {
        when(repo.findByLotIdAndFloor(10L, 1)).thenReturn(List.of(spot));
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        List<SpotResponseDTO> result = service.getSpotsByFloor(10L, 1);

        assertEquals(1, result.size());
    }

    @Test
    void shouldGetEVSpotsSuccessfully() {
        when(repo.findByLotIdAndIsEVChargingTrue(10L)).thenReturn(List.of(spot));
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        List<SpotResponseDTO> result = service.getEVSpots(10L);

        assertEquals(1, result.size());
    }

    @Test
    void shouldGetHandicappedSpotsSuccessfully() {
        when(repo.findByLotIdAndIsHandicappedTrue(10L)).thenReturn(List.of(spot));
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        List<SpotResponseDTO> result = service.getHandicappedSpots(10L);

        assertEquals(1, result.size());
    }

    @Test
    void shouldSkipDuplicatesInBulkCreate() {
        BulkSpotRequestDTO dto = new BulkSpotRequestDTO();
        dto.setLotId(10L);
        dto.setPrefix("A");
        dto.setFloor(1);
        dto.setCount(2);
        dto.setSpotType(SpotType.COMPACT);
        dto.setVehicleType(VehicleType.FOUR_WHEELER);
        dto.setPricePerHour(50.0);

        // First spot duplicate, second is new
        when(repo.existsByLotIdAndSpotNumber(10L, "A1-01")).thenReturn(true);
        when(repo.existsByLotIdAndSpotNumber(10L, "A1-02")).thenReturn(false);
        when(repo.saveAll(anyList())).thenReturn(List.of(spot));
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        List<SpotResponseDTO> result = service.addBulkSpots(dto);

        assertEquals(1, result.size()); // only 1 new spot saved
    }

    @Test
    void shouldUpdateSpotSuccessfully() {
        when(repo.findById(1L)).thenReturn(java.util.Optional.of(spot));
        when(repo.save(spot)).thenReturn(spot);
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        SpotResponseDTO result = service.updateSpot(1L, requestDTO);

        assertNotNull(result);
        verify(repo).save(spot);
    }
}