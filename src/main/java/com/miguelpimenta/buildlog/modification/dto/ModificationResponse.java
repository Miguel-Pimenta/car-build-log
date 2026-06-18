package com.miguelpimenta.buildlog.modification.dto;

import com.miguelpimenta.buildlog.modification.ModificationCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Outbound representation of a modification. */
public record ModificationResponse(
        UUID id,
        UUID vehicleId,
        ModificationCategory category,
        String name,
        String partNumber,
        BigDecimal cost,
        LocalDate installedAt,
        int mileageKmAtInstall,
        Instant createdAt
) {
}
