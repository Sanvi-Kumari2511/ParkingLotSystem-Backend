package com.parkease.parkinglot.service;

import com.parkease.parkinglot.dto.request.ParkingLotRequestDTO;
import com.parkease.parkinglot.dto.response.ParkingLotResponseDTO;

import java.util.List;

public interface ParkingLotService {

    // ── Manager operations ────────────────────────────────────────────────────
    ParkingLotResponseDTO createLot(ParkingLotRequestDTO dto, String managerEmail);
    ParkingLotResponseDTO updateLot(Long id, ParkingLotRequestDTO dto, String managerEmail);
    void deleteLot(Long id, String managerEmail);
    ParkingLotResponseDTO toggleOpen(Long id, String managerEmail);
    List<ParkingLotResponseDTO> getLotsByManager(String managerEmail);

    // ── Public / Driver operations ────────────────────────────────────────────
    ParkingLotResponseDTO getLotById(Long id);
    List<ParkingLotResponseDTO> getByCity(String city);
    List<ParkingLotResponseDTO> getNearbyLots(double lat, double lon, double radiusKm);
    List<ParkingLotResponseDTO> getOpenLots();

    // ── Admin operations ──────────────────────────────────────────────────────
    ParkingLotResponseDTO approveLot(Long id, String feedback, String performerEmail);
    ParkingLotResponseDTO rejectLot(Long id, String feedback, String performerEmail);
    List<ParkingLotResponseDTO> getPendingApprovalLots();
    List<ParkingLotResponseDTO> getAllApprovedLots();

    // ── Internal (called by booking-service) ─────────────────────────────────
    void decrementSpot(Long lotId);
    void incrementSpot(Long lotId);
}
