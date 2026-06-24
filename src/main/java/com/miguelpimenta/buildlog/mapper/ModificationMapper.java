package com.miguelpimenta.buildlog.mapper;

import com.miguelpimenta.buildlog.dto.ModificationRequest;
import com.miguelpimenta.buildlog.dto.ModificationResponse;
import com.miguelpimenta.buildlog.model.Modification;
import com.miguelpimenta.buildlog.model.Vehicle;
import org.springframework.stereotype.Component;

/** Maps between {@link Modification} entities and their DTOs. */
@Component
public class ModificationMapper {

  public Modification toEntity(ModificationRequest request, Vehicle vehicle) {
    Modification modification = new Modification();
    modification.setVehicle(vehicle);
    modification.setCategory(request.category());
    modification.setName(request.name());
    modification.setPartNumber(request.partNumber());
    modification.setCost(request.cost());
    modification.setInstalledAt(request.installedAt());
    modification.setMileageKmAtInstall(request.mileageKmAtInstall());
    return modification;
  }

  public ModificationResponse toResponse(Modification modification) {
    return new ModificationResponse(
        modification.getId(),
        modification.getVehicle().getId(),
        modification.getCategory(),
        modification.getName(),
        modification.getPartNumber(),
        modification.getCost(),
        modification.getInstalledAt(),
        modification.getMileageKmAtInstall(),
        modification.getCreatedAt());
  }
}
