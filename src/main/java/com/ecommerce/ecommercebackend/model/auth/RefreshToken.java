package com.ecommerce.ecommercebackend.model.auth;

import com.ecommerce.ecommercebackend.model.base.BaseEntity;
import com.ecommerce.ecommercebackend.model.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing a Refresh Token in the database. Used to maintain stateful control over
 * long-lived sessions, allowing the server to forcefully invalidate (logout) users if necessary.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
public class RefreshToken extends BaseEntity {

    // A single user can have multiple active sessions (e.g., mobile app and web browser)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Stores the actual JWT string. Length is set to 512 to accommodate long token signatures.
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    // The exact expiration timestamp. Useful for validation and automated database cleanup.
    @Column(nullable = false)
    private Instant expiryDate;

    // Positive logic flag indicating if the token is currently valid.
    // Defaults to true upon creation. Set to false during logout or security breaches.
    @Column(nullable = false)
    private boolean active = true;
}
