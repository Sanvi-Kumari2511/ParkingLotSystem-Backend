package com.parkease.booking.dto.response;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Represents a parking spot as seen by a Drive-In user.
 *
 * Three possible availability states:
 *
 *  FREE              – No existing booking; fully selectable.
 *  RESERVED_AVAILABLE– A future pre-booking exists, but the slot can still be
 *                      used by a drive-in driver until that reservation starts.
 *                      availabilityLabel = "Available until HH:mm".
 *  OCCUPIED          – Spot is actively occupied or under maintenance;
 *                      NOT selectable.
 *
 * Similar to how BookMyShow shows "available" seats up to a showtime cutoff.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriveInSpotDTO {

    /** Spot identifier */
    private Long   spotId;

    /** Human-readable spot number (e.g. "A1-03") */
    private String spotNumber;

    /** Floor level inside the parking lot */
    private int    floor;

    /** REGULAR, COMPACT, PREMIUM, etc. */
    private String spotType;

    /** CAR, BIKE, TRUCK, etc. */
    private String vehicleType;

    /**
     * Computed availability status for the drive-in view:
     *   FREE | RESERVED_AVAILABLE | OCCUPIED | MAINTENANCE
     */
    private String status;

    private double  pricePerHour;
    private boolean isEVCharging;
    private boolean isHandicapped;

    /**
     * Whether the drive-in user can select this spot right now.
     * False when the spot is OCCUPIED or under MAINTENANCE.
     */
    private boolean selectable;

    /**
     * Human-readable availability hint shown in the UI:
     *   "Available Now"              → FREE
     *   "Available until HH:mm"     → RESERVED_AVAILABLE (reservation kicks in later)
     *   "Currently Occupied"        → OCCUPIED
     *   "Under Maintenance"         → MAINTENANCE
     */
    private String availabilityLabel;

    /**
     * ISO timestamp of when the next pre-booking on this spot starts.
     * Null for FREE spots.  Drive-in bookings must end before this time.
     */
    private LocalDateTime reservedFrom;
}
