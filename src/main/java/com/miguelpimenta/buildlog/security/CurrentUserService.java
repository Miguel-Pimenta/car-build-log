package com.miguelpimenta.buildlog.security;

import com.miguelpimenta.buildlog.model.User;
import com.miguelpimenta.buildlog.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
  private final UserRepository userRepository;

  public CurrentUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User getCurrentUser() {
    // The JWT filter stored the principal here earlier in the request; read its
    // username back out.
    String username = SecurityContextHolder.getContext().getAuthentication().getName();

    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + username));
  }
}
