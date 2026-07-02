package com.miguelpimenta.buildlog.exception;

/**
 * Thrown by the service layer when an entity cannot be found. Mapped to a 404 response by {@link
 * GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }

  public static ResourceNotFoundException of(String resource, Object id) {
    return new ResourceNotFoundException("%s not found: %s".formatted(resource, id));
  }
}
