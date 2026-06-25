package com.miguelpimenta.buildlog.mapper;

import com.miguelpimenta.buildlog.dto.VehicleRequest;
import com.miguelpimenta.buildlog.dto.VehicleResponse;
import com.miguelpimenta.buildlog.model.Vehicle;
import org.springframework.stereotype.Component;

/** Maps between {@link Vehicle} entities and their DTOs. */
@Component
public class VehicleMapper {

  public Vehicle toEntity(VehicleRequest request) {
    Vehicle vehicle = new Vehicle();
    apply(request, vehicle);
    return vehicle;
  }

  /** Copies request fields onto an existing (or new) entity - used for create and update. */
  public void apply(VehicleRequest request, Vehicle vehicle) {
    vehicle.setMake(request.make());
    vehicle.setModel(request.model());
    vehicle.setYear(request.year());
    vehicle.setEngineCode(request.engineCode());
    vehicle.setStatus(request.status());
    vehicle.setNotes(request.notes());
  }

  public VehicleResponse toResponse(Vehicle vehicle) {
    return new VehicleResponse(
        vehicle.getId(),
        vehicle.getMake(),
        vehicle.getModel(),
        vehicle.getYear(),
        vehicle.getEngineCode(),
        vehicle.getStatus(),
        vehicle.getNotes(),
        vehicle.getCreatedAt());
  }
}
