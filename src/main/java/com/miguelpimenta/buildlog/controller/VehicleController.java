package com.miguelpimenta.buildlog.controller;

import com.miguelpimenta.buildlog.dto.VehicleRequest;
import com.miguelpimenta.buildlog.dto.VehicleResponse;
import com.miguelpimenta.buildlog.exception.PageResponse;
import com.miguelpimenta.buildlog.model.VehicleStatus;
import com.miguelpimenta.buildlog.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vehicles", description = "Create, browse, and manage the vehicles you own.")
public class VehicleController {

  private final VehicleService vehicleService;

  public VehicleController(VehicleService vehicleService) {
    this.vehicleService = vehicleService;
  }

  @Operation(
      summary = "Create a vehicle",
      description = "Registers a new vehicle owned by the authenticated user.")
  @PostMapping
  // @Valid triggers the bean-validation annotations on VehicleRequest;
  // @RequestBody deserialises
  // the JSON.
  public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest request) {
    VehicleResponse created = vehicleService.create(request);
    // Build the new resource's URL for the 201 Location header (REST convention for
    // "here's what I
    // made").
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();
    return ResponseEntity.created(location).body(created);
  }

  @Operation(
      summary = "List vehicles",
      description = "Paginated list of your vehicles, with optional search and status filters.")
  @GetMapping
  public PageResponse<VehicleResponse> list(
      // @PageableDefault supplies page/size/sort defaults when the client omits them
      // (?page=&size=&sort=).
      @PageableDefault(size = 20) Pageable pageable,
      // required=false makes these query params optional; Spring auto-converts the
      // status string to
      // the enum.
      @RequestParam(required = false) String search,
      @RequestParam(required = false) VehicleStatus status) {
    return PageResponse.from(vehicleService.list(search, status, pageable));
  }

  @Operation(summary = "Get a vehicle by id")
  @GetMapping("/{id}")
  public VehicleResponse get(@PathVariable UUID id) {
    return vehicleService.get(id);
  }

  @Operation(summary = "Update a vehicle")
  @PutMapping("/{id}")
  public VehicleResponse update(@PathVariable UUID id, @Valid @RequestBody VehicleRequest request) {
    return vehicleService.update(id, request);
  }

  @Operation(summary = "Delete a vehicle")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    vehicleService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
