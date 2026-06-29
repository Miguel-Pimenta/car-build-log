package com.miguelpimenta.buildlog.controller;

import com.miguelpimenta.buildlog.dto.VehicleRequest;
import com.miguelpimenta.buildlog.dto.VehicleResponse;
import com.miguelpimenta.buildlog.exception.PageResponse;
import com.miguelpimenta.buildlog.model.VehicleStatus;
import com.miguelpimenta.buildlog.service.VehicleService;
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
public class VehicleController {

  private final VehicleService vehicleService;

  public VehicleController(VehicleService vehicleService) {
    this.vehicleService = vehicleService;
  }

  @PostMapping
  public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest request) {
    VehicleResponse created = vehicleService.create(request);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(created.id())
        .toUri();
    return ResponseEntity.created(location).body(created);
  }

  @GetMapping
  public PageResponse<VehicleResponse> list(
      @PageableDefault(size = 20) Pageable pageable,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) VehicleStatus status) {
    return PageResponse.from(vehicleService.list(search, status, pageable));
  }

  @GetMapping("/{id}")
  public VehicleResponse get(@PathVariable UUID id) {
    return vehicleService.get(id);
  }

  @PutMapping("/{id}")
  public VehicleResponse update(@PathVariable UUID id, @Valid @RequestBody VehicleRequest request) {
    return vehicleService.update(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    vehicleService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
