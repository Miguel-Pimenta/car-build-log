package com.miguelpimenta.buildlog.service;

import com.miguelpimenta.buildlog.dto.VehicleSummaryResponse;
import com.miguelpimenta.buildlog.dto.VehicleSummaryResponse.DynoSnapshot;
import com.miguelpimenta.buildlog.model.DynoResult;
import com.miguelpimenta.buildlog.model.Modification;
import com.miguelpimenta.buildlog.model.ModificationCategory;
import com.miguelpimenta.buildlog.model.Vehicle;
import com.miguelpimenta.buildlog.repository.DynoResultRepository;
import com.miguelpimenta.buildlog.repository.ModificationRepository;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the derived {@code /summary} view: counts modifications, sums spend
 * (overall and per category), and pulls the latest dyno figures. This is the
 * one piece of genuine business logic beyond CRUD, and the part worth unit
 * testing thoroughly.
 */
@Service
@Transactional(readOnly = true)
public class VehicleSummaryService {

    private final VehicleService vehicleService;
    private final ModificationRepository modificationRepository;
    private final DynoResultRepository dynoResultRepository;

    public VehicleSummaryService(VehicleService vehicleService,
                                 ModificationRepository modificationRepository,
                                 DynoResultRepository dynoResultRepository) {
        this.vehicleService = vehicleService;
        this.modificationRepository = modificationRepository;
        this.dynoResultRepository = dynoResultRepository;
    }

    public VehicleSummaryResponse summarise(UUID vehicleId) {
        Vehicle vehicle = vehicleService.getEntity(vehicleId); // 404 if the vehicle does not exist

        List<Modification> mods = modificationRepository.findByVehicleId(vehicleId);

        BigDecimal totalSpend = mods.stream().map(Modification::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);

        // EnumMap keeps the breakdown in the enum's declared order for stable output.
        Map<ModificationCategory, BigDecimal> spendByCategory = new EnumMap<>(ModificationCategory.class);
        for (Modification mod : mods) {
            spendByCategory.merge(mod.getCategory(), mod.getCost(), BigDecimal::add);
        }

        DynoResult latest = dynoResultRepository.findFirstByVehicleIdOrderByMeasuredAtDescCreatedAtDesc(vehicleId).orElse(null);

        DynoSnapshot latestDyno = (latest == null) ? null : new DynoSnapshot(latest.getPowerHp(), latest.getTorqueNm(), latest.getMeasuredAt());
        Integer currentPowerHp = (latest == null) ? null : latest.getPowerHp();
        Integer currentTorqueNm = (latest == null) ? null : latest.getTorqueNm();

        return new VehicleSummaryResponse(vehicle.getId(),mods.size(),totalSpend,spendByCategory,latestDyno,currentPowerHp,currentTorqueNm);
    }
}
