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
      // @Value injects config properties (from application.yml / env vars) into
      // constructor params.
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-ms}") long expirationMs) {
    // Turn the secret string into an HMAC-SHA signing key; the same key both signs
    // and verifies.
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
        // signWith appends the HMAC signature; compact() serialises to the
        // header.payload.signature
        // string.
        .signWith(key)
        .compact();
  }

  public String extractUsername(String token) {
    // verifyWith(key) rejects tampered/forged tokens before we read any claim (the
    // subject).
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
  }

  public boolean isTokenValid(String token, String username) {
    try {
      var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      return username.equals(claims.getSubject()) && claims.getExpiration().after(new Date());
    } catch (JwtException | IllegalArgumentException ex) {
      // Bad signature, malformed, or expired token: treat as invalid rather than
      // throwing.
      return false;
    }
  }
}
