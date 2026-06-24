package com.miguelpimenta.buildlog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miguelpimenta.buildlog.dto.VehicleRequest;
import com.miguelpimenta.buildlog.dto.VehicleResponse;
import com.miguelpimenta.buildlog.exception.ResourceNotFoundException;
import com.miguelpimenta.buildlog.mapper.VehicleMapper;
import com.miguelpimenta.buildlog.model.Vehicle;
import com.miguelpimenta.buildlog.repository.VehicleRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

  @Mock VehicleRepository vehicleRepository;

  VehicleService vehicleService;

  @BeforeEach
  void setUp() {
    // Real mapper, mocked repository - we want to verify the mapping too.
    vehicleService = new VehicleService(vehicleRepository, new VehicleMapper());
  }

  @Test
  void createMapsAndPersists() {
    when(vehicleRepository.save(any(Vehicle.class)))
        .thenAnswer(
            invocation -> {
              Vehicle v = invocation.getArgument(0);
              v.setId(UUID.randomUUID());
              return v;
            });

    VehicleRequest request =
        new VehicleRequest("Volkswagen", "Golf", 2015, "EA288", "daily driver");
    VehicleResponse response = vehicleService.create(request);

    assertThat(response.id()).isNotNull();
    assertThat(response.make()).isEqualTo("Volkswagen");
    assertThat(response.engineCode()).isEqualTo("EA288");
  }

  @Test
  void getThrowsNotFoundWhenMissing() {
    UUID id = UUID.randomUUID();
    when(vehicleRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> vehicleService.get(id))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining(id.toString());
  }

  @Test
  void deleteThrowsNotFoundAndDoesNotDeleteWhenMissing() {
    UUID id = UUID.randomUUID();
    when(vehicleRepository.existsById(id)).thenReturn(false);

    assertThatThrownBy(() -> vehicleService.delete(id))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(vehicleRepository, never()).deleteById(id);
  }

  @Test
  void updateAppliesChangesWhenPresent() {
    UUID id = UUID.randomUUID();
    Vehicle existing = new Vehicle();
    existing.setId(id);
    existing.setMake("VW");
    existing.setModel("Golf");
    existing.setYear(2010);
    existing.setEngineCode("CBFA");
    when(vehicleRepository.findById(id)).thenReturn(Optional.of(existing));

    VehicleRequest request = new VehicleRequest("Volkswagen", "Golf R", 2018, "EA888", null);
    VehicleResponse response = vehicleService.update(id, request);

    assertThat(response.make()).isEqualTo("Volkswagen");
    assertThat(response.model()).isEqualTo("Golf R");
    assertThat(response.year()).isEqualTo(2018);
    assertThat(response.engineCode()).isEqualTo("EA888");
  }
}
