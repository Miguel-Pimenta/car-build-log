package com.miguelpimenta.buildlog.dto;

import com.miguelpimenta.buildlog.model.ModificationCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregated view of a build: how many modifications, how much was spent (overall and broken down
 * by category), and the latest dyno figures if any have been recorded.
 */
public record VehicleSummaryResponse(
    UUID vehicleId,
    long totalModifications,
    BigDecimal totalSpend,
    Map<ModificationCategory, BigDecimal> spendByCategory,
    DynoSnapshot latestDyno,
    Integer currentPowerHp,
    Integer currentTorqueNm) {
  /** The most recent dyno measurement; null when none has been recorded. */
  public record DynoSnapshot(int powerHp, int torqueNm, LocalDate measuredAt) {}
}
