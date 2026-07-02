package com.miguelpimenta.buildlog.repository;

import com.miguelpimenta.buildlog.model.DynoResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DynoResultRepository extends JpaRepository<DynoResult, UUID> {

  // Derived query: Spring Data parses the method name into SQL, no @Query needed.
  /** All dyno results for a vehicle, newest measurement first. */
  List<DynoResult> findByVehicleIdOrderByMeasuredAtDesc(UUID vehicleId);

  /** The single most recent dyno result, used by the build summary. */
  Optional<DynoResult> findFirstByVehicleIdOrderByMeasuredAtDescCreatedAtDesc(UUID vehicleId);
}
