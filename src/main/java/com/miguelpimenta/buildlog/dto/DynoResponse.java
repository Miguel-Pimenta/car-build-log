package com.miguelpimenta.buildlog.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Outbound representation of a dyno result. */
public record DynoResponse(
        UUID id,
        UUID vehicleId,
        int powerHp,
        int torqueNm,
        LocalDate measuredAt,
        String notes,
        Instant createdAt
) {
}
