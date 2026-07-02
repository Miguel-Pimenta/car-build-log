package com.miguelpimenta.buildlog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single modification made to a {@link Vehicle} - e.g. "Stage 1 remap". Many modifications belong
 * to one vehicle.
 */
@Entity
@Table(name = "modifications")
@Getter
@Setter
@NoArgsConstructor
public class Modification {

  @Id
  // Random UUID primary key generated on persist (see Vehicle for the pattern).
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  // Persist the enum by name, not ordinal, so the enum can be reordered without
  // corrupting rows.
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ModificationCategory category;

  @Column(nullable = false)
  private String name;

  @Column(name = "part_number")
  private String partNumber;

  // precision/scale map to SQL DECIMAL(12,2): up to 12 digits, 2 after the point
  // - exact money, no
  // rounding drift.
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal cost;

  @Column(name = "installed_at", nullable = false)
  private LocalDate installedAt;

  @Column(name = "mileage_km_at_install", nullable = false)
  private int mileageKmAtInstall;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  // Owning side of the many-to-one: this table holds the vehicle_id FK; LAZY
  // defers loading the
  // parent.
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "vehicle_id", nullable = false)
  private Vehicle vehicle;

  // Runs before the first INSERT to set the creation timestamp automatically.
  @PrePersist
  void onCreate() {
    this.createdAt = Instant.now();
  }
}
