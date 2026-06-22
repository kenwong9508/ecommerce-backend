package com.ecommerce.ecommercebackend.security;

import com.ecommerce.ecommercebackend.model.user.User;
import com.ecommerce.ecommercebackend.repository.user.UserRepository;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom service to load user-specific data during the authentication process. Acts as a bridge
 * between your application's database and Spring Security.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Locates the user based on the username. In our system, the "username" used for login is the
     * email. * @param email The email identifying the user whose data is required.
     *
     * @return A fully populated CustomUserDetails object containing user information and authorities.
     * @throws UsernameNotFoundException if the user could not be found.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // 1. Fetch the user from the database using the email
        log.debug("Attempting to load user by email: {}", email);
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // 2. Convert the user's Roles into Spring Security GrantedAuthorities
        // Magic trick: We automatically prepend "ROLE_" here so that @PreAuthorize("hasRole('ADMIN')")
        // works flawlessly later.
        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());

        // 3. Construct and return our customized ID card (CustomUserDetails)
        return new CustomUserDetails(
                user.getId(), user.getEmail(), user.getUsername(), user.getPassword(), authorities);
    }
}
