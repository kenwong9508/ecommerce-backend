package com.ecommerce.ecommercebackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that intercepts incoming HTTP requests to validate JWT Access Tokens. If a valid token is
 * found, it populates the Spring Security Context.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1. Extract token from the Authorization header
            String jwt = getJwtFromRequest(request);

            // 2. Validate token integrity and expiration
            if (StringUtils.hasText(jwt) && tokenProvider.validateAccessToken(jwt)) {

                // 3. Extract username and load user details
                String username = tokenProvider.getUsernameFromAccessToken(jwt);
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                // 4. Create an authentication token (credentials are null as JWT is already validated)
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                // Attach request details (IP, Session) for auditing purposes
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 5. Save the authentication object in the Security Context (marks as authenticated)
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // Log the error but do not throw it, allowing the SecurityConfig to handle the rejection
            log.error("Could not set user authentication in security context: {}", ex.getMessage());
        }

        // 6. Pass the request to the next filter in the chain
        filterChain.doFilter(request, response);
    }

    /** Helper method to parse the "Bearer <token>" string from the header. */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
