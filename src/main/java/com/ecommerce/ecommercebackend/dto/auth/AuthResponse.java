package com.ecommerce.ecommercebackend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for returning the generated JWTs back to the client. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

  private String accessToken;
  private String refreshToken;

  @Builder.Default private String tokenType = "Bearer";
}
