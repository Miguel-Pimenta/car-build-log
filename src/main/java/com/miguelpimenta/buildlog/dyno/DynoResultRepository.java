package com.miguelpimenta.buildlog.dyno;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DynoResultRepository extends JpaRepository<DynoResult, UUID> {

    /** All dyno results for a vehicle, newest measurement first. */
    List<DynoResult> findByVehicleIdOrderByMeasuredAtDesc(UUID vehicleId);

    /** The single most recent dyno result, used by the build summary. */
    Optional<DynoResult> findFirstByVehicleIdOrderByMeasuredAtDescCreatedAtDesc(UUID vehicleId);
}
