package com.miguelpimenta.buildlog.dto;

import java.time.Instant;
import java.util.UUID;

/** Outbound representation of a vehicle. */
public record VehicleResponse(
    UUID id,
    String make,
    String model,
    int year,
    String engineCode,
    String notes,
    Instant createdAt) {}
