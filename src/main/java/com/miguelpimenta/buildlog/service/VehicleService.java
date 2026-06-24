package com.miguelpimenta.buildlog.service;

import com.miguelpimenta.buildlog.dto.VehicleRequest;
import com.miguelpimenta.buildlog.dto.VehicleResponse;
import com.miguelpimenta.buildlog.exception.ResourceNotFoundException;
import com.miguelpimenta.buildlog.mapper.VehicleMapper;
import com.miguelpimenta.buildlog.model.Vehicle;
import com.miguelpimenta.buildlog.repository.VehicleRepository;
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

  public VehicleService(VehicleRepository vehicleRepository, VehicleMapper vehicleMapper) {
    this.vehicleRepository = vehicleRepository;
    this.vehicleMapper = vehicleMapper;
  }

  @Transactional
  public VehicleResponse create(VehicleRequest request) {
    Vehicle saved = vehicleRepository.save(vehicleMapper.toEntity(request));
    return vehicleMapper.toResponse(saved);
  }

  public Page<VehicleResponse> list(Pageable pageable) {
    return vehicleRepository.findAll(pageable).map(vehicleMapper::toResponse);
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
    if (!vehicleRepository.existsById(id)) {
      throw ResourceNotFoundException.of("Vehicle", id);
    }
    vehicleRepository.deleteById(id);
  }

  /**
   * Loads a vehicle or throws 404. Shared with the modification, dyno and summary services so the
   * not-found behaviour lives in one place.
   */
  public Vehicle getEntity(UUID id) {
    return vehicleRepository
        .findById(id)
        .orElseThrow(() -> ResourceNotFoundException.of("Vehicle", id));
  }
}
