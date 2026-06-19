package com.miguelpimenta.buildlog.service;

import com.miguelpimenta.buildlog.dto.DynoRequest;
import com.miguelpimenta.buildlog.dto.DynoResponse;
import com.miguelpimenta.buildlog.mapper.DynoMapper;
import com.miguelpimenta.buildlog.model.DynoResult;
import com.miguelpimenta.buildlog.model.Vehicle;
import com.miguelpimenta.buildlog.repository.DynoResultRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DynoService {

    private final DynoResultRepository dynoResultRepository;
    private final VehicleService vehicleService;
    private final DynoMapper dynoMapper;

    public DynoService(DynoResultRepository dynoResultRepository, VehicleService vehicleService, DynoMapper dynoMapper) {
        this.dynoResultRepository = dynoResultRepository;
        this.vehicleService = vehicleService;
        this.dynoMapper = dynoMapper;
    }

    @Transactional
    public DynoResponse addToVehicle(UUID vehicleId, DynoRequest request) {
        Vehicle vehicle = vehicleService.getEntity(vehicleId);
        DynoResult saved = dynoResultRepository.save(dynoMapper.toEntity(request, vehicle));
        return dynoMapper.toResponse(saved);
    }

    public List<DynoResponse> listForVehicle(UUID vehicleId) {
        vehicleService.getEntity(vehicleId); // 404 if the vehicle does not exist
        return dynoResultRepository.findByVehicleIdOrderByMeasuredAtDesc(vehicleId).stream().map(dynoMapper::toResponse).toList();
    }
}
