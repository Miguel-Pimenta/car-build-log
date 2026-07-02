package com.miguelpimenta.buildlog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

  @Id
  // Random UUID primary key generated on persist.
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private String name;

  @Column(unique = true, nullable = false)
  private String email;

  @Column(unique = true, nullable = false)
  private String username;

  // Stores the BCrypt hash, never the raw password.
  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  // Persist role by name; defaults to USER for newly created accounts.
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Role role = Role.USER;
}
