package com.miguelpimenta.buildlog.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.miguelpimenta.buildlog.common.ResourceNotFoundException;
import com.miguelpimenta.buildlog.dyno.DynoResult;
import com.miguelpimenta.buildlog.dyno.DynoResultRepository;
import com.miguelpimenta.buildlog.modification.Modification;
import com.miguelpimenta.buildlog.modification.ModificationCategory;
import com.miguelpimenta.buildlog.modification.ModificationRepository;
import com.miguelpimenta.buildlog.vehicle.dto.VehicleSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The summary aggregation is the only non-CRUD business logic, so it gets the
 * most thorough unit coverage. Repositories are mocked so the test exercises
 * the arithmetic, not the database.
 */
@ExtendWith(MockitoExtension.class)
class VehicleSummaryServiceTest {

    @Mock
    VehicleService vehicleService;
    @Mock
    ModificationRepository modificationRepository;
    @Mock
    DynoResultRepository dynoResultRepository;

    @InjectMocks
    VehicleSummaryService summaryService;

    private final UUID vehicleId = UUID.randomUUID();

    @Test
    void aggregatesSpendCountsAndLatestDyno() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        when(vehicleService.getEntity(vehicleId)).thenReturn(vehicle);

        when(modificationRepository.findByVehicleId(vehicleId)).thenReturn(List.of(
                mod(ModificationCategory.ENGINE, "120.00"),
                mod(ModificationCategory.ENGINE, "80.50"),
                mod(ModificationCategory.EXHAUST, "300.00")
        ));

        DynoResult dyno = new DynoResult();
        dyno.setPowerHp(240);
        dyno.setTorqueNm(420);
        dyno.setMeasuredAt(LocalDate.of(2025, 5, 1));
        when(dynoResultRepository.findFirstByVehicleIdOrderByMeasuredAtDescCreatedAtDesc(vehicleId))
                .thenReturn(Optional.of(dyno));

        VehicleSummaryResponse summary = summaryService.summarise(vehicleId);

        assertThat(summary.vehicleId()).isEqualTo(vehicleId);
        assertThat(summary.totalModifications()).isEqualTo(3);
        assertThat(summary.totalSpend()).isEqualByComparingTo(new BigDecimal("500.50"));
        assertThat(summary.spendByCategory())
                .containsEntry(ModificationCategory.ENGINE, new BigDecimal("200.50"))
                .containsEntry(ModificationCategory.EXHAUST, new BigDecimal("300.00"));
        assertThat(summary.latestDyno()).isNotNull();
        assertThat(summary.latestDyno().powerHp()).isEqualTo(240);
        assertThat(summary.latestDyno().torqueNm()).isEqualTo(420);
        assertThat(summary.currentPowerHp()).isEqualTo(240);
        assertThat(summary.currentTorqueNm()).isEqualTo(420);
    }

    @Test
    void handlesEmptyBuildWithNoModificationsOrDyno() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        when(vehicleService.getEntity(vehicleId)).thenReturn(vehicle);
        when(modificationRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(dynoResultRepository.findFirstByVehicleIdOrderByMeasuredAtDescCreatedAtDesc(vehicleId))
                .thenReturn(Optional.empty());

        VehicleSummaryResponse summary = summaryService.summarise(vehicleId);

        assertThat(summary.totalModifications()).isZero();
        assertThat(summary.totalSpend()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.spendByCategory()).isEmpty();
        assertThat(summary.latestDyno()).isNull();
        assertThat(summary.currentPowerHp()).isNull();
        assertThat(summary.currentTorqueNm()).isNull();
    }

    @Test
    void propagatesNotFoundWhenVehicleMissing() {
        when(vehicleService.getEntity(vehicleId))
                .thenThrow(ResourceNotFoundException.of("Vehicle", vehicleId));

        assertThatThrownBy(() -> summaryService.summarise(vehicleId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Modification mod(ModificationCategory category, String cost) {
        Modification modification = new Modification();
        modification.setCategory(category);
        modification.setCost(new BigDecimal(cost));
        return modification;
    }
}
