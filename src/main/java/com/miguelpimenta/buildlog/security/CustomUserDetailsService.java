package com.miguelpimenta.buildlog.security;

import com.miguelpimenta.buildlog.model.User;
import com.miguelpimenta.buildlog.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads users from the database for Spring Security authentication. */
@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  public CustomUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  // Spring Security calls this to look up an account; we adapt our own User
  // entity into its
  // UserDetails contract.
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    // "ROLE_" prefix is the Spring convention that lets hasRole("USER") checks
    // match this
    // authority.
    return org.springframework.security.core.userdetails.User.builder()
        .username(user.getUsername())
        .password(user.getPasswordHash())
        .authorities("ROLE_" + user.getRole().name())
        .build();
  }
}
