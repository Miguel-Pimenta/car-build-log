package com.miguelpimenta.buildlog.model;

/**
 * Authorisation role for a {@link User}. Stored as a string (see {@code @Enumerated}).
 *
 * <p>Every account registers as {@link #USER}; {@link #ADMIN} is reserved and currently unreachable
 * — nothing assigns it and no endpoint checks for it. {@code CustomUserDetailsService} already
 * publishes the role as a {@code ROLE_} authority, so adding an admin-only endpoint would be a
 * matter of assigning the role and guarding the route, not new plumbing.
 */
public enum Role {
  USER,
  ADMIN
}
