package com.miguelpimenta.buildlog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A dyno (rolling-road) measurement for a {@link Vehicle}, capturing peak power and torque on a
 * given date.
 */
@Entity
@Table(name = "dyno_results")
@Getter
@Setter
@NoArgsConstructor
public class DynoResult {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "vehicle_id", nullable = false)
  private Vehicle vehicle;

  @Column(name = "power_hp", nullable = false)
  private int powerHp;

  @Column(name = "torque_nm", nullable = false)
  private int torqueNm;

  @Column(name = "measured_at", nullable = false)
  private LocalDate measuredAt;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    this.createdAt = Instant.now();
  }
}
