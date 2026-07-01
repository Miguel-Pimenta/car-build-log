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
import com.miguelpimenta.buildlog.model.User;
import com.miguelpimenta.buildlog.model.Vehicle;
import com.miguelpimenta.buildlog.model.VehicleStatus;
import com.miguelpimenta.buildlog.repository.VehicleRepository;
import com.miguelpimenta.buildlog.security.CurrentUserService;
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
  @Mock CurrentUserService currentUserService;

  VehicleService vehicleService;

  User currentUser;

  @BeforeEach
  void setUp() {
    // Real mapper, mocked repository - we want to verify the mapping too.
    vehicleService = new VehicleService(vehicleRepository, new VehicleMapper(), currentUserService);

    // The authenticated user every ownership-aware path resolves to.
    currentUser = new User();
    currentUser.setId(UUID.randomUUID());
  }

  @Test
  void createMapsAndPersists() {
    when(currentUserService.getCurrentUser()).thenReturn(currentUser);
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
    // findById is empty, so getEntity throws before the ownership check runs.
    when(vehicleRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> vehicleService.get(id))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining(id.toString());
  }

  @Test
  void getThrowsNotFoundWhenOwnedByAnotherUser() {
    UUID id = UUID.randomUUID();
    User otherUser = new User();
    otherUser.setId(UUID.randomUUID());
    Vehicle vehicle = new Vehicle();
    vehicle.setId(id);
    vehicle.setOwner(otherUser);
    when(vehicleRepository.findById(id)).thenReturn(Optional.of(vehicle));
    when(currentUserService.getCurrentUser()).thenReturn(currentUser);

    // The vehicle exists but belongs to someone else -> same 404 to avoid leaking existence.
    assertThatThrownBy(() -> vehicleService.get(id))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining(id.toString());
  }

  @Test
  void deleteThrowsNotFoundAndDoesNotDeleteWhenMissing() {
    UUID id = UUID.randomUUID();
    // delete now loads via getEntity; a missing vehicle throws before any delete call.
    when(vehicleRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> vehicleService.delete(id))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(vehicleRepository, never()).delete(any());
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
    existing.setOwner(currentUser);
    when(vehicleRepository.findById(id)).thenReturn(Optional.of(existing));
    when(currentUserService.getCurrentUser()).thenReturn(currentUser);

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
    vehicle.setOwner(currentUser);

    when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    // Surrounding whitespace is trimmed, and the same term feeds both conditions.
    when(vehicleRepository.search("Vol", VehicleStatus.DAILY, currentUser, pageable))
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
    vehicle.setOwner(currentUser);

    when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    // No search and no status: the service passes null for both and the @Query
    // handles "filter absent" itself, so findAll is never used.
    when(vehicleRepository.search(null, null, currentUser, pageable))
        .thenReturn(new PageImpl<>(List.of(vehicle)));

    Page<VehicleResponse> result = vehicleService.list(null, null, pageable);

    assertThat(result).hasSize(1);
    assertThat(result.getContent().get(0).model()).isEqualTo("M3");
    verify(vehicleRepository, never()).findAll(any(Pageable.class));
  }

  @Test
  void listWithBlankSearchNormalisesToNull() {
    Pageable pageable = PageRequest.of(0, 20);
    when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    // Whitespace-only search is blank, so the service normalises it to null
    // (not " ") before querying.
    when(vehicleRepository.search(null, null, currentUser, pageable))
        .thenReturn(new PageImpl<>(List.of()));

    vehicleService.list("   ", null, pageable);

    verify(vehicleRepository).search(null, null, currentUser, pageable);
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
    vehicle.setOwner(currentUser);

    when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    // No search term, just a status filter -> search(null, SOLD, owner, pageable).
    when(vehicleRepository.search(null, VehicleStatus.SOLD, currentUser, pageable))
        .thenReturn(new PageImpl<>(List.of(vehicle)));

    Page<VehicleResponse> result = vehicleService.list(null, VehicleStatus.SOLD, pageable);

    assertThat(result).hasSize(1);
    assertThat(result.getContent().get(0).model()).isEqualTo("Supra");
    verify(vehicleRepository, never()).findAll(any(Pageable.class));
  }
}
