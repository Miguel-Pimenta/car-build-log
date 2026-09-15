package com.miguelpimenta.buildlog.service;

import com.miguelpimenta.buildlog.dto.ModificationRequest;
import com.miguelpimenta.buildlog.dto.ModificationResponse;
import com.miguelpimenta.buildlog.exception.ResourceNotFoundException;
import com.miguelpimenta.buildlog.mapper.ModificationMapper;
import com.miguelpimenta.buildlog.model.Modification;
import com.miguelpimenta.buildlog.model.Vehicle;
import com.miguelpimenta.buildlog.repository.ModificationRepository;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
// Read-only transactions by default; write methods below opt in with a plain
// @Transactional.
@Transactional(readOnly = true)
public class ModificationService {

  private final ModificationRepository modificationRepository;
  private final VehicleService vehicleService;
  private final ModificationMapper modificationMapper;

  public ModificationService(
      ModificationRepository modificationRepository,
      VehicleService vehicleService,
      ModificationMapper modificationMapper) {
    this.modificationRepository = modificationRepository;
    this.vehicleService = vehicleService;
    this.modificationMapper = modificationMapper;
  }

  @Transactional
  public ModificationResponse addToVehicle(UUID vehicleId, ModificationRequest request) {
    // Reuse VehicleService.getEntity so the same 404/ownership check guards this
    // nested resource.
    Vehicle vehicle = vehicleService.getEntity(vehicleId);
    Modification saved = modificationRepository.save(modificationMapper.toEntity(request, vehicle));

    log.info("Added modification {} to vehicle {}", saved.getId(), vehicleId);
    return modificationMapper.toResponse(saved);
  }

  public List<ModificationResponse> listForVehicle(UUID vehicleId) {
    vehicleService.getEntity(vehicleId);

    log.info("Listing modifications for vehicle {}", vehicleId);
    return modificationRepository.findByVehicleId(vehicleId).stream()
        .map(modificationMapper::toResponse)
        .toList();
  }

  @Transactional
  public void delete(UUID id) {
    // Go through getEntity so the ownership check below runs. existsById +
    // deleteById would skip it, because neither ever loads the row.
    Modification modification = getEntity(id);
    modificationRepository.delete(modification);
    log.info("Deleted modification {}", id);
  }

  public ModificationResponse get(UUID id) {
    return modificationMapper.toResponse(getEntity(id));
  }

  private Modification getEntity(UUID id) {
    Modification modification =
        modificationRepository
            .findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Modification", id));

    // The flat /modifications/{id} routes never look a vehicle up, so they would
    // otherwise bypass the owner check that guards the nested routes. Re-run it
    // here against the parent vehicle: someone else's modification is reported as
    // 404, matching VehicleService.getEntity.
    vehicleService.getEntity(modification.getVehicle().getId());

    return modification;
  }
}
