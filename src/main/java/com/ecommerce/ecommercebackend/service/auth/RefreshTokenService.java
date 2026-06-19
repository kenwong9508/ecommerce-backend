package com.ecommerce.ecommercebackend.service.auth;

import com.ecommerce.ecommercebackend.model.auth.RefreshToken;
import java.util.Optional;

/** Interface for Refresh Token lifecycle management operations. */
public interface RefreshTokenService {

  RefreshToken createAndSaveRefreshToken(Long userId, String tokenString);

  Optional<RefreshToken> findByToken(String tokenString);

  RefreshToken verifyExpirationAndStatus(RefreshToken token);

  void revokeToken(String tokenString);
}
