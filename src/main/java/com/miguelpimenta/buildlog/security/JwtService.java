package com.miguelpimenta.buildlog.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Creates and validates HS256 JWTs using the JJWT 0.13 API. The signing key is
 * derived from the
 * {@code app.jwt.secret} property, which must be at least 32 bytes for HS256.
 */
@Service
public class JwtService {

  private final SecretKey key;
  private final long expirationMs;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-ms}") long expirationMs) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
  }

  public String generateToken(String username) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + expirationMs);
    return Jwts.builder()
        .subject(username)
        .issuedAt(now)
        .expiration(expiration)
        .signWith(key)
        .compact();
  }

  public String extractUsername(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
  }

  public boolean isTokenValid(String token, String username) {
    try {
      var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      return username.equals(claims.getSubject()) && claims.getExpiration().after(new Date());
    } catch (JwtException | IllegalArgumentException ex) {
      return false;
    }
  }
}
