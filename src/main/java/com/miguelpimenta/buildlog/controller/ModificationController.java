package com.miguelpimenta.buildlog.controller;

import com.miguelpimenta.buildlog.dto.ModificationRequest;
import com.miguelpimenta.buildlog.dto.ModificationResponse;
import com.miguelpimenta.buildlog.service.ModificationService;
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
public class ModificationController {

    private final ModificationService modificationService;

    public ModificationController(ModificationService modificationService) {
        this.modificationService = modificationService;
    }

    @PostMapping("/vehicles/{vehicleId}/modifications")
    public ResponseEntity<ModificationResponse> add(@PathVariable UUID vehicleId, @Valid @RequestBody ModificationRequest request) {
        ModificationResponse created = modificationService.addToVehicle(vehicleId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/modifications/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/vehicles/{vehicleId}/modifications")
    public List<ModificationResponse> listForVehicle(@PathVariable UUID vehicleId) {
        return modificationService.listForVehicle(vehicleId);
    }

    @GetMapping("/modifications/{id}")
    public ModificationResponse get(@PathVariable UUID id) {
        return modificationService.get(id);
    }

    @DeleteMapping("/modifications/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        modificationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
