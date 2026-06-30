package com.miguelpimenta.buildlog.repository;

import com.miguelpimenta.buildlog.model.Vehicle;
import com.miguelpimenta.buildlog.model.VehicleStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
  @Query(
      """
        SELECT v FROM Vehicle v
        WHERE (:status IS NULL OR v.status = :status)
          AND (:search IS NULL
               OR LOWER(v.make)  LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')))
      """)
  Page<Vehicle> search(String search, VehicleStatus status, Pageable pageable);
}
