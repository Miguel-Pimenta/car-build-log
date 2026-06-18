package com.miguelpimenta.buildlog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Inbound payload for recording a dyno result. */
public record DynoRequest(

        @Positive
        int powerHp,

        @Positive
        int torqueNm,

        @NotNull
        @PastOrPresent
        LocalDate measuredAt,

        @Size(max = 2000)
        String notes
) {
}
