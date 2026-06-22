package com.ecommerce.ecommercebackend.repository.auth;

import com.ecommerce.ecommercebackend.model.auth.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RefreshToken entity. Provides standard database operations
 * (CRUD) and custom queries for token management.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Retrieves a refresh token entity from the database using its exact JWT string. Wrapped in an
     * Optional to safely and elegantly handle cases where the token does not exist.
     *
     * @param token The literal JWT string of the refresh token.
     * @return An Optional containing the RefreshToken if found, or empty if not found.
     */
    Optional<RefreshToken> findByToken(String token);
}
