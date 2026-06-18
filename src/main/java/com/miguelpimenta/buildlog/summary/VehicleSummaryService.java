package com.miguelpimenta.buildlog.summary;

import com.miguelpimenta.buildlog.dyno.DynoResult;
import com.miguelpimenta.buildlog.dyno.DynoResultRepository;
import com.miguelpimenta.buildlog.modification.Modification;
import com.miguelpimenta.buildlog.modification.ModificationCategory;
import com.miguelpimenta.buildlog.modification.ModificationRepository;
import com.miguelpimenta.buildlog.summary.dto.VehicleSummaryResponse;
import com.miguelpimenta.buildlog.summary.dto.VehicleSummaryResponse.DynoSnapshot;
import com.miguelpimenta.buildlog.vehicle.Vehicle;
import com.miguelpimenta.buildlog.vehicle.VehicleService;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the derived {@code /summary} view by composing data from the vehicle,
 * modification and dyno modules. This is the one piece of genuine business
 * logic beyond CRUD, and the part worth unit testing thoroughly.
 *
 * <p>It lives in its own module so that {@code vehicle} stays free of any
 * dependency on {@code modification}/{@code dyno} - the summary depends on them,
 * not the other way around, which keeps the module graph acyclic.
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

        BigDecimal totalSpend = mods.stream()
                .map(Modification::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // EnumMap keeps the breakdown in the enum's declared order for stable output.
        Map<ModificationCategory, BigDecimal> spendByCategory = new EnumMap<>(ModificationCategory.class);
        for (Modification mod : mods) {
            spendByCategory.merge(mod.getCategory(), mod.getCost(), BigDecimal::add);
        }

        DynoResult latest = dynoResultRepository
                .findFirstByVehicleIdOrderByMeasuredAtDescCreatedAtDesc(vehicleId)
                .orElse(null);

        DynoSnapshot latestDyno = (latest == null) ? null
                : new DynoSnapshot(latest.getPowerHp(), latest.getTorqueNm(), latest.getMeasuredAt());
        Integer currentPowerHp = (latest == null) ? null : latest.getPowerHp();
        Integer currentTorqueNm = (latest == null) ? null : latest.getTorqueNm();

        return new VehicleSummaryResponse(
                vehicle.getId(),
                mods.size(),
                totalSpend,
                spendByCategory,
                latestDyno,
                currentPowerHp,
                currentTorqueNm
        );
    }
}
