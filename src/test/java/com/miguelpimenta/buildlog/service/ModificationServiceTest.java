package com.miguelpimenta.buildlog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miguelpimenta.buildlog.exception.ResourceNotFoundException;
import com.miguelpimenta.buildlog.mapper.ModificationMapper;
import com.miguelpimenta.buildlog.model.Modification;
import com.miguelpimenta.buildlog.model.ModificationCategory;
import com.miguelpimenta.buildlog.model.Vehicle;
import com.miguelpimenta.buildlog.repository.ModificationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModificationServiceTest {

  @Mock ModificationRepository modificationRepository;
  @Mock VehicleService vehicleService;

  ModificationService modificationService;

  UUID vehicleId;
  UUID modificationId;
  Modification modification;

  @BeforeEach
  void setUp() {
    // Real mapper, mocked repository - we want to verify the mapping too.
    modificationService =
        new ModificationService(modificationRepository, vehicleService, new ModificationMapper());

    vehicleId = UUID.randomUUID();
    modificationId = UUID.randomUUID();

    Vehicle vehicle = new Vehicle();
    vehicle.setId(vehicleId);

    modification = new Modification();
    modification.setId(modificationId);
    modification.setVehicle(vehicle);
    modification.setCategory(ModificationCategory.TUNING);
    modification.setName("Stage 2 ECU remap");
    modification.setCost(new BigDecimal("720.00"));
    modification.setInstalledAt(LocalDate.of(2025, 4, 5));
  }

  @Test
  void getReturnsModificationWhenTheVehicleIsOwnedByTheCaller() {
    when(modificationRepository.findById(modificationId)).thenReturn(Optional.of(modification));

    assertThat(modificationService.get(modificationId).name()).isEqualTo("Stage 2 ECU remap");

    // The owner check still has to run on the happy path, not just the failure one.
    verify(vehicleService).getEntity(vehicleId);
  }

  @Test
  void getThrowsNotFoundWhenTheVehicleBelongsToAnotherUser() {
    when(modificationRepository.findById(modificationId)).thenReturn(Optional.of(modification));
    // VehicleService.getEntity is the ownership gate: it 404s on someone else's vehicle.
    when(vehicleService.getEntity(vehicleId))
        .thenThrow(ResourceNotFoundException.of("Vehicle", vehicleId));

    // The modification exists, but the caller doesn't own its vehicle -> 404, so we
    // never confirm it exists (IDOR defense, same as VehicleService).
    assertThatThrownBy(() -> modificationService.get(modificationId))
        .isInstanceOf(ResourceNotFoundException.class)
        // The message must name the ID the caller asked for and must not mention the
        // vehicle, otherwise it leaks the parent ID and doubles as an existence oracle:
        // a missing modification would report a different message than someone else's.
        .hasMessageContaining(modificationId.toString())
        .hasMessageNotContaining(vehicleId.toString());
  }

  @Test
  void deleteThrowsNotFoundWhenTheVehicleBelongsToAnotherUser() {
    when(modificationRepository.findById(modificationId)).thenReturn(Optional.of(modification));
    when(vehicleService.getEntity(vehicleId))
        .thenThrow(ResourceNotFoundException.of("Vehicle", vehicleId));

    assertThatThrownBy(() -> modificationService.delete(modificationId))
        .isInstanceOf(ResourceNotFoundException.class);

    // The assertion that actually catches a regression: nothing was deleted.
    verify(modificationRepository, never()).delete(any());
    verify(modificationRepository, never()).deleteById(any());
  }

  @Test
  void deleteRemovesTheModificationWhenTheVehicleIsOwnedByTheCaller() {
    when(modificationRepository.findById(modificationId)).thenReturn(Optional.of(modification));

    modificationService.delete(modificationId);

    verify(modificationRepository).delete(modification);
  }

  @Test
  void getThrowsNotFoundWhenTheModificationDoesNotExist() {
    when(modificationRepository.findById(modificationId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> modificationService.get(modificationId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining(modificationId.toString());
  }
}
