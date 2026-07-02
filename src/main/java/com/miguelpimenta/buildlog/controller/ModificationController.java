package com.miguelpimenta.buildlog.controller;

import com.miguelpimenta.buildlog.dto.ModificationRequest;
import com.miguelpimenta.buildlog.dto.ModificationResponse;
import com.miguelpimenta.buildlog.service.ModificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Modifications", description = "Add and manage the modifications on a vehicle.")
public class ModificationController {

  private final ModificationService modificationService;

  public ModificationController(ModificationService modificationService) {
    this.modificationService = modificationService;
  }

  @Operation(summary = "Add a modification to a vehicle")
  @PostMapping("/vehicles/{vehicleId}/modifications")
  public ResponseEntity<ModificationResponse> add(
      @PathVariable UUID vehicleId, @Valid @RequestBody ModificationRequest request) {
    ModificationResponse created = modificationService.addToVehicle(vehicleId, request);
    // Created under /vehicles/{id}/modifications but the canonical URL is
    // /modifications/{id}, so build from the context root.
    URI location =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/v1/modifications/{id}")
            .buildAndExpand(created.id())
            .toUri();
    return ResponseEntity.created(location).body(created);
  }

  @Operation(summary = "List a vehicle's modifications")
  @GetMapping("/vehicles/{vehicleId}/modifications")
  public List<ModificationResponse> listForVehicle(@PathVariable UUID vehicleId) {
    return modificationService.listForVehicle(vehicleId);
  }

  @Operation(summary = "Get a modification by id")
  @GetMapping("/modifications/{id}")
  public ModificationResponse get(@PathVariable UUID id) {
    return modificationService.get(id);
  }

  @Operation(summary = "Delete a modification")
  @DeleteMapping("/modifications/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    modificationService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
