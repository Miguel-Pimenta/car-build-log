package com.miguelpimenta.buildlog.service;

import com.miguelpimenta.buildlog.dto.VehicleRequest;
import com.miguelpimenta.buildlog.dto.VehicleResponse;
import com.miguelpimenta.buildlog.exception.ResourceNotFoundException;
import com.miguelpimenta.buildlog.mapper.VehicleMapper;
import com.miguelpimenta.buildlog.model.User;
import com.miguelpimenta.buildlog.model.Vehicle;
import com.miguelpimenta.buildlog.model.VehicleStatus;
import com.miguelpimenta.buildlog.repository.VehicleRepository;
import com.miguelpimenta.buildlog.security.CurrentUserService;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VehicleService {

  private final VehicleRepository vehicleRepository;
  private final VehicleMapper vehicleMapper;
  private final CurrentUserService currentUserService;

  public VehicleService(VehicleRepository vehicleRepository, VehicleMapper vehicleMapper,
      CurrentUserService currentUserService) {
    this.vehicleRepository = vehicleRepository;
    this.vehicleMapper = vehicleMapper;
    this.currentUserService = currentUserService;
  }

  @Transactional
  public VehicleResponse create(VehicleRequest request) {
    Vehicle vehicle = vehicleMapper.toEntity(request);
    vehicle.setOwner(currentUserService.getCurrentUser());

    Vehicle saved = vehicleRepository.save(vehicle);

    return vehicleMapper.toResponse(saved);
  }

  public Page<VehicleResponse> list(String search, VehicleStatus status, Pageable pageable) {
    String term = (search != null && !search.isBlank()) ? search.trim() : null;
    User owner = currentUserService.getCurrentUser();

    return vehicleRepository.search(term, status, owner, pageable).map(vehicleMapper::toResponse);
  }

  public VehicleResponse get(UUID id) {
    return vehicleMapper.toResponse(getEntity(id));
  }

  @Transactional
  public VehicleResponse update(UUID id, VehicleRequest request) {
    Vehicle vehicle = getEntity(id);
    vehicleMapper.apply(request, vehicle);
    // Managed entity: JPA flushes the changes on commit, no explicit save needed.
    return vehicleMapper.toResponse(vehicle);
  }

  @Transactional
  public void delete(UUID id) {
    Vehicle vehicle = getEntity(id);
    vehicleRepository.delete(vehicle);
  }

  /**
   * Loads a vehicle or throws 404. Shared with the modification, dyno and summary
   * services so the
   * not-found behaviour lives in one place.
   */
  public Vehicle getEntity(UUID id) {
    Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Vehicle", id));
    UUID currentUserId = currentUserService.getCurrentUser().getId();

    if (!vehicle.getOwner().getId().equals(currentUserId)) {
      throw ResourceNotFoundException.of("Vehicle", id);
    }

    return vehicle;
  }
}
