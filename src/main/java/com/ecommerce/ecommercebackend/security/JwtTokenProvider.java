package com.ecommerce.ecommercebackend.security;

import com.ecommerce.ecommercebackend.config.JwtProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  private final JwtProperties jwtProperties;

  // Explicitly named secret keys to follow enterprise naming standards
  private SecretKey accessTokenSecretKey;
  private SecretKey refreshTokenSecretKey;

  /**
   * Initializes cryptographic keys immediately after dependency injection is complete. Decodes the
   * Base64 configured string secrets from application.yml and converts them into secure SecretKey
   * instances suitable for HMAC-SHA signature algorithms.
   */
  @PostConstruct
  public void init() {
    byte[] accessKeyBytes = Decoders.BASE64.decode(jwtProperties.getAccessTokenSecret());
    this.accessTokenSecretKey = Keys.hmacShaKeyFor(accessKeyBytes);

    byte[] refreshKeyBytes = Decoders.BASE64.decode(jwtProperties.getRefreshTokenSecret());
    this.refreshTokenSecretKey = Keys.hmacShaKeyFor(refreshKeyBytes);
  }

  /**
   * Generates a short-lived Access Token containing the user's identity, roles, and metadata. *
   * Note on identity mapping: - Standard "subject" (sub) = userPrincipal.getUsername() -> Maps to
   * the User's Email (Login Identifier) - Custom claim "username" =
   * userPrincipal.getActualUsername() -> Maps to the Database Profile Username
   */
  public String generateAccessToken(Authentication authentication) {
    CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();

    // Convert Spring Security GrantedAuthority objects into plain String roles (e.g.,
    // ["ROLE_USER"])
    List<String> roles =
        userPrincipal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

    return Jwts.builder()
        .subject(userPrincipal.getUsername()) // Standard Claim: "sub" (User's login Email)
        .claim("userId", userPrincipal.getId()) // Custom Claim: Database User ID
        .claim(
            "username",
            userPrincipal.getActualUsername()) // Custom Claim: Profile Username (e.g., peter_chan)
        .claim("roles", roles) // Custom Claim: Assigned Security Roles
        .issuedAt(now) // Standard Claim: "iat"
        .expiration(expiryDate) // Standard Claim: "exp"
        .signWith(accessTokenSecretKey) // Signed cryptographically with the Access Key
        .compact();
  }

  /**
   * Generates a long-lived Refresh Token with minimal public claims. Contains only the standard
   * subject (email) to allow session re-evaluation during rotation.
   */
  public String generateRefreshToken(Authentication authentication) {
    CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();

    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

    return Jwts.builder()
        .subject(userPrincipal.getUsername()) // Standard Claim: "sub" (User's login Email)
        .issuedAt(now) // Standard Claim: "iat"
        .expiration(expiryDate) // Standard Claim: "exp"
        .signWith(refreshTokenSecretKey) // Signed cryptographically with the Refresh Key
        .compact();
  }

  /**
   * Extracts the subject (login email) from a given Access Token. This operation enforces
   * structural validation and verifies the signature using the Access Key.
   */
  public String getUsernameFromAccessToken(String token) {
    return Jwts.parser()
        .verifyWith(accessTokenSecretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }

  /**
   * Entry point to validate the integrity, signature, and expiration of an incoming Access Token.
   */
  public boolean validateAccessToken(String authToken) {
    return validateToken(authToken, accessTokenSecretKey);
  }

  /**
   * Entry point to validate the integrity, signature, and expiration of an incoming Refresh Token.
   */
  public boolean validateRefreshToken(String authToken) {
    return validateToken(authToken, refreshTokenSecretKey);
  }

  /**
   * Unified internal helper method that parses and verifies a token against a designated key.
   * Catches and logs all potential JWT structural, signature, or lifecycle anomalies.
   */
  private boolean validateToken(String authToken, SecretKey secretKey) {
    try {
      Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(authToken);
      return true;
    } catch (SecurityException | MalformedJwtException e) {
      log.error("Invalid JWT signature structure or corrupt formatting: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      log.error("JWT token lifecycle has expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      log.error("JWT token capabilities or algorithm unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      log.error("JWT claims payload is empty or unreadable: {}", e.getMessage());
    }
    return false;
  }
}
