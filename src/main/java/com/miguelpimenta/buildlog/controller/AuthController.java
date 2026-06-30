package com.miguelpimenta.buildlog.controller;

import com.miguelpimenta.buildlog.dto.AuthResponse;
import com.miguelpimenta.buildlog.dto.LoginRequest;
import com.miguelpimenta.buildlog.dto.RegisterRequest;
import com.miguelpimenta.buildlog.model.Role;
import com.miguelpimenta.buildlog.model.User;
import com.miguelpimenta.buildlog.repository.UserRepository;
import com.miguelpimenta.buildlog.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Public authentication endpoints: register a new user and log in to obtain a
 * JWT.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  public AuthController(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      AuthenticationManager authenticationManager) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.authenticationManager = authenticationManager;
  }

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    if (userRepository.existsByUsername(request.username())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
    }
    if (userRepository.existsByEmail(request.email())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
    }

    User user = new User();
    user.setUsername(request.username());
    user.setEmail(request.email());
    user.setName(request.name());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setRole(Role.USER);
    userRepository.save(user);

    String token = jwtService.generateToken(user.getUsername());
    return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.username(), request.password()));
    } catch (AuthenticationException ex) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }
    String token = jwtService.generateToken(request.username());
    return ResponseEntity.ok(new AuthResponse(token));
  }
}
