package com.miguelpimenta.buildlog.dto;

import com.miguelpimenta.buildlog.model.VehicleStatus;
import java.time.Instant;
import java.util.UUID;

/** Outbound representation of a vehicle. */
public record VehicleResponse(
    UUID id,
    String make,
    String model,
    int year,
    String engineCode,
    VehicleStatus status,
    String notes,
    String owner,
    Instant createdAt) {}
