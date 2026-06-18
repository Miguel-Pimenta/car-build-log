package com.miguelpimenta.buildlog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound payload for creating or updating a vehicle. Validated with
 * {@code @Valid} at the controller boundary.
 */
public record VehicleRequest(

        @NotBlank
        @Size(max = 100)
        String make,

        @NotBlank
        @Size(max = 100)
        String model,

        @Min(1900)
        @Max(2100)
        int year,

        @NotBlank
        @Size(max = 50)
        String engineCode,

        @Size(max = 2000)
        String notes
) {
}
