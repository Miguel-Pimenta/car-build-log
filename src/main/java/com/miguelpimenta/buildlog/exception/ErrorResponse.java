package com.miguelpimenta.buildlog.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Consistent error body returned for every handled exception: {@code { "timestamp", "status",
 * "error", "message" }}. Validation failures additionally carry a {@code fieldErrors} map; it is
 * omitted when empty.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    Instant timestamp, int status, String error, String message, Map<String, String> fieldErrors) {
  public static ErrorResponse of(HttpStatus status, String message) {
    return of(status, message, null);
  }

  public static ErrorResponse of(
      HttpStatus status, String message, Map<String, String> fieldErrors) {
    return new ErrorResponse(
        Instant.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
  }
}
