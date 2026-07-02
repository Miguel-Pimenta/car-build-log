package com.miguelpimenta.buildlog.repository;

import com.miguelpimenta.buildlog.model.User;
import com.miguelpimenta.buildlog.model.Vehicle;
import com.miguelpimenta.buildlog.model.VehicleStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// Extending JpaRepository gives CRUD + paging for free; only custom queries need to be declared.
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
  // Optional filters: "(:x IS NULL OR ...)" means a null param disables that
  // condition,
  // so one query serves all combinations of search/status without dynamic query
  // building.
  @Query("""
        SELECT v FROM Vehicle v
        WHERE v.owner = :owner
          AND (:status IS NULL OR v.status = :status)
          AND (:search IS NULL
               OR LOWER(v.make)  LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')))
      """)
  Page<Vehicle> search(String search, VehicleStatus status, User owner, Pageable pageable);
}
