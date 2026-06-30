package com.miguelpimenta.buildlog.dto;

/** Outbound payload carrying a freshly issued JWT. */
public record AuthResponse(String token) {
}
