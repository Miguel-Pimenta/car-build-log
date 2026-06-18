package com.miguelpimenta.buildlog.vehicle;

import com.miguelpimenta.buildlog.vehicle.dto.VehicleSummaryResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles/{id}/summary")
public class VehicleSummaryController {

    private final VehicleSummaryService summaryService;

    public VehicleSummaryController(VehicleSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping
    public VehicleSummaryResponse summary(@PathVariable UUID id) {
        return summaryService.summarise(id);
    }
}
