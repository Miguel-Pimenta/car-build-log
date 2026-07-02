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
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A project car being tracked in the build log. Parent of its modifications and dyno results. */
@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
public class Vehicle {

  @Id
  // Hibernate generates a random UUID primary key on persist (no DB sequence / auto-increment).
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String make;

  @Column(nullable = false)
  private String model;

  @Column(name = "model_year", nullable = false)
  private int year;

  @Column(name = "engine_code", nullable = false)
  private String engineCode;

  // EnumType.STRING stores the enum's name (e.g. "PROJECT") not its ordinal, so reordering the enum
  // is safe.
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private VehicleStatus status;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  // Many vehicles to one owner. LAZY = don't load the User until accessed; optional=false = FK is
  // required.
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  // @JoinColumn names the foreign-key column (owner_id) on this (the "many") side's table.
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  // @PrePersist: Hibernate callback that runs just before the first INSERT, stamping the creation
  // time.
  @PrePersist
  void onCreate() {
    this.createdAt = Instant.now();
  }
}
