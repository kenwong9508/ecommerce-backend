package com.ecommerce.ecommercebackend.security;

import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Custom implementation of Spring Security's UserDetails. Extends the default functionality to
 * store additional user attributes (like ID and actual username) so they can be easily embedded
 * into the JWT payload.
 */
@Getter
@AllArgsConstructor // Automatically generates a constructor requiring all declared fields
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email; // Used as the "username" for Spring Security authentication
    private final String actualUsername; // The actual username from the database
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    // --- Standard Spring Security Overrides ---
    /**
     * Spring Security calls this method during authentication to identify the user. Since users log
     * in with their email, we return the email here.
     */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // Note: The 4 boolean methods (isAccountNonExpired, etc.) are intentionally omitted.
    // Spring Security 6+ provides default interface methods that return true.
}
