package com.miguelpimenta.buildlog.repository;

import com.miguelpimenta.buildlog.model.Vehicle;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
  Page<Vehicle> findByMakeContainingIgnoreCaseOrModelContainingIgnoreCase(
      String make, String model, Pageable pageable);
}
