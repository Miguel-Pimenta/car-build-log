package com.miguelpimenta.buildlog.controller;

import com.miguelpimenta.buildlog.dto.DynoRequest;
import com.miguelpimenta.buildlog.dto.DynoResponse;
import com.miguelpimenta.buildlog.service.DynoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/dyno")
@Tag(name = "Dyno results", description = "Record and list dyno pulls for a vehicle.")
public class DynoController {

  private final DynoService dynoService;

  public DynoController(DynoService dynoService) {
    this.dynoService = dynoService;
  }

  @Operation(summary = "Add a dyno result")
  // No single-result GET is exposed, so we return 201 + body without a Location
  // header.
  @PostMapping
  public ResponseEntity<DynoResponse> add(
      @PathVariable UUID vehicleId, @Valid @RequestBody DynoRequest request) {
    DynoResponse created = dynoService.addToVehicle(vehicleId, request);
    return ResponseEntity.status(201).body(created);
  }

  @Operation(summary = "List a vehicle's dyno results")
  @GetMapping
  public List<DynoResponse> list(@PathVariable UUID vehicleId) {
    return dynoService.listForVehicle(vehicleId);
  }
}
