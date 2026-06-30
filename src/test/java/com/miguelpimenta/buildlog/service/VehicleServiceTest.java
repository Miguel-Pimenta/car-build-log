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
import com.miguelpimenta.buildlog.model.VehicleStatus;
import com.miguelpimenta.buildlog.repository.VehicleRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
        new VehicleRequest(
            "Volkswagen", "Golf", 2015, "EA288", VehicleStatus.PROJECT, "daily driver");
    VehicleResponse response = vehicleService.create(request);

    assertThat(response.id()).isNotNull();
    assertThat(response.make()).isEqualTo("Volkswagen");
    assertThat(response.engineCode()).isEqualTo("EA288");
    assertThat(response.status()).isEqualTo(VehicleStatus.PROJECT);
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

    VehicleRequest request =
        new VehicleRequest("Volkswagen", "Golf R", 2018, "EA888", VehicleStatus.DAILY, null);
    VehicleResponse response = vehicleService.update(id, request);

    assertThat(response.make()).isEqualTo("Volkswagen");
    assertThat(response.model()).isEqualTo("Golf R");
    assertThat(response.year()).isEqualTo(2018);
    assertThat(response.engineCode()).isEqualTo("EA888");
    assertThat(response.status()).isEqualTo(VehicleStatus.DAILY);
  }

  @Test
  void listWithSearchTermTrimsAndQueriesByMakeOrModel() {
    Pageable pageable = PageRequest.of(0, 20);
    Vehicle vehicle = new Vehicle();
    vehicle.setId(UUID.randomUUID());
    vehicle.setMake("Volkswagen");
    vehicle.setModel("Golf");
    vehicle.setYear(2015);
    vehicle.setEngineCode("EA288");

    // Surrounding whitespace is trimmed, and the same term feeds both conditions.
    when(vehicleRepository.search("Vol", VehicleStatus.DAILY, pageable))
        .thenReturn(new PageImpl<>(List.of(vehicle)));

    Page<VehicleResponse> result = vehicleService.list("  Vol  ", VehicleStatus.DAILY, pageable);

    assertThat(result).hasSize(1);
    assertThat(result.getContent().get(0).make()).isEqualTo("Volkswagen");
    verify(vehicleRepository, never()).findAll(any(Pageable.class));
  }

  @Test
  void listWithoutAnyFilterQueriesWithNulls() {
    Pageable pageable = PageRequest.of(0, 20);
    Vehicle vehicle = new Vehicle();
    vehicle.setId(UUID.randomUUID());
    vehicle.setMake("BMW");
    vehicle.setModel("M3");
    vehicle.setYear(2010);
    vehicle.setEngineCode("S54");

    // No search and no status: the service passes null for both and the @Query
    // handles "filter absent" itself, so findAll is never used.
    when(vehicleRepository.search(null, null, pageable))
        .thenReturn(new PageImpl<>(List.of(vehicle)));

    Page<VehicleResponse> result = vehicleService.list(null, null, pageable);

    assertThat(result).hasSize(1);
    assertThat(result.getContent().get(0).model()).isEqualTo("M3");
    verify(vehicleRepository, never()).findAll(any(Pageable.class));
  }

  @Test
  void listWithBlankSearchNormalisesToNull() {
    Pageable pageable = PageRequest.of(0, 20);
    // Whitespace-only search is blank, so the service normalises it to null
    // (not " ") before querying.
    when(vehicleRepository.search(null, null, pageable)).thenReturn(new PageImpl<>(List.of()));

    vehicleService.list("   ", null, pageable);

    verify(vehicleRepository).search(null, null, pageable);
    verify(vehicleRepository, never()).findAll(any(Pageable.class));
  }

  @Test
  void listWithStatusOnlyQueriesByStatus() {
    Pageable pageable = PageRequest.of(0, 20);
    Vehicle vehicle = new Vehicle();
    vehicle.setId(UUID.randomUUID());
    vehicle.setMake("Toyota");
    vehicle.setModel("Supra");
    vehicle.setYear(1998);
    vehicle.setEngineCode("2JZ-GTE");

    // No search term, just a status filter -> search(null, SOLD, pageable).
    when(vehicleRepository.search(null, VehicleStatus.SOLD, pageable))
        .thenReturn(new PageImpl<>(List.of(vehicle)));

    Page<VehicleResponse> result = vehicleService.list(null, VehicleStatus.SOLD, pageable);

    assertThat(result).hasSize(1);
    assertThat(result.getContent().get(0).model()).isEqualTo("Supra");
    verify(vehicleRepository, never()).findAll(any(Pageable.class));
  }
}
