package com.miguelpimenta.buildlog.dyno;

import com.miguelpimenta.buildlog.dyno.dto.DynoRequest;
import com.miguelpimenta.buildlog.dyno.dto.DynoResponse;
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
public class DynoController {

    private final DynoService dynoService;

    public DynoController(DynoService dynoService) {
        this.dynoService = dynoService;
    }

    // No single-result GET is exposed, so we return 201 + body without a Location header.
    @PostMapping
    public ResponseEntity<DynoResponse> add(@PathVariable UUID vehicleId,
                                            @Valid @RequestBody DynoRequest request) {
        DynoResponse created = dynoService.addToVehicle(vehicleId, request);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping
    public List<DynoResponse> list(@PathVariable UUID vehicleId) {
        return dynoService.listForVehicle(vehicleId);
    }
}
