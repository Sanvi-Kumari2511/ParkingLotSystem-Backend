package com.parkease.vehicle.dto.request;
 
import com.parkease.vehicle.entity.VehicleType;
import jakarta.validation.constraints.*;
import lombok.Data;
 
@Data
public class VehicleRequestDTO {
 
    @NotBlank(message = "License plate is required")
    @Pattern(regexp = "^[A-Z]{2}[0-9]{1,2}[A-Z]{0,2}[0-9]{4}$", message = "Invalid Indian vehicle number plate format")
    private String licensePlate;
 
    @NotBlank(message = "Make is required (e.g. Toyota)")
    private String make;
 
    @NotBlank(message = "Model is required (e.g. Camry)")
    private String model;
 
    private String color;
 
    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;
 
    private boolean isEV = false;
}