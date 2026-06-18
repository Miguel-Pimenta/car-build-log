package com.miguelpimenta.buildlog.repository;

import com.miguelpimenta.buildlog.model.Modification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModificationRepository extends JpaRepository<Modification, UUID> {

    /** All modifications for a vehicle - used by the listing and summary endpoints. */
    List<Modification> findByVehicleId(UUID vehicleId);
}
