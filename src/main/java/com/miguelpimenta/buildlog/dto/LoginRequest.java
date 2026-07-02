package com.miguelpimenta.buildlog.dto;

import jakarta.validation.constraints.NotBlank;

/** Inbound payload for authenticating with username and password. */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
