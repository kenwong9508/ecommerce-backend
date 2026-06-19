package com.ecommerce.ecommercebackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when a refresh token is invalid, expired, or revoked. Triggers a 403
 * Forbidden status code by default.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class TokenRefreshException extends RuntimeException {

  public TokenRefreshException(String message) {
    super(message);
  }
}
