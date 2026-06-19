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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ModificationService {

    private final ModificationRepository modificationRepository;
    private final VehicleService vehicleService;
    private final ModificationMapper modificationMapper;

    public ModificationService(ModificationRepository modificationRepository,
                               VehicleService vehicleService,
                               ModificationMapper modificationMapper) {
        this.modificationRepository = modificationRepository;
        this.vehicleService = vehicleService;
        this.modificationMapper = modificationMapper;
    }

    @Transactional
    public ModificationResponse addToVehicle(UUID vehicleId, ModificationRequest request) {
        Vehicle vehicle = vehicleService.getEntity(vehicleId);
        Modification saved = modificationRepository.save(modificationMapper.toEntity(request, vehicle));
        
        return modificationMapper.toResponse(saved);
    }

    public List<ModificationResponse> listForVehicle(UUID vehicleId) {
        vehicleService.getEntity(vehicleId); 

        return modificationRepository.findByVehicleId(vehicleId).stream().map(modificationMapper::toResponse).toList();
    }

    @Transactional
    public void delete(UUID id) {
        if (!modificationRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Modification", id);
        }
        modificationRepository.deleteById(id);
    }

    public ModificationResponse get(UUID id) {
        return modificationMapper.toResponse(getEntity(id));
    }

    private Modification getEntity(UUID id) {
        return modificationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Modification", id));
    }
}
