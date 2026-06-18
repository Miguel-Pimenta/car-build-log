package com.miguelpimenta.buildlog.dto;

import com.miguelpimenta.buildlog.model.ModificationCategory;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Inbound payload for adding a modification to a vehicle. */
public record ModificationRequest(

        @NotNull
        ModificationCategory category,

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 100)
        String partNumber,

        @NotNull
        @PositiveOrZero
        @Digits(integer = 10, fraction = 2)
        BigDecimal cost,

        @NotNull
        @PastOrPresent
        LocalDate installedAt,

        @PositiveOrZero
        int mileageKmAtInstall
) {
}
