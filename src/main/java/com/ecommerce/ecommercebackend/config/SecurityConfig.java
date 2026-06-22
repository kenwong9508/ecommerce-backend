package com.ecommerce.ecommercebackend.config;

import com.ecommerce.ecommercebackend.security.JwtAuthEntryPoint;
import com.ecommerce.ecommercebackend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity // Enables Spring Security's web security support
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final JwtAuthEntryPoint jwtAuthEntryPoint;

  /**
   * Exposes the AuthenticationManager as a Bean so it can be injected into AuthServiceImpl. This is
   * required in Spring Security 6+ as it is no longer exposed by default.
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  /**
   * Defines the password hashing algorithm. BCrypt is the industry standard for securely storing
   * and verifying passwords.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Configures the core security filter chain. Sets up CSRF, session management, and routing
   * authorization rules.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // 1. Disable CSRF protection (since we are building a REST API communicating via JSON
        // and using stateless JWTs, traditional form-based CSRF defense is not needed)
        .csrf(AbstractHttpConfigurer::disable)

        // 2. Set session management to STATELESS
        // Instructs Spring Security not to create or use HTTP sessions for storing the user's
        // security context
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // 3. Delegate unauthorized (401) errors to our custom Entry Point
        .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(jwtAuthEntryPoint))

        // 4. Configure API access rules
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/v1/auth/**")
                    .permitAll() // Allow public access to Login/Register endpoints
                    .requestMatchers("/error")
                    .permitAll() // Allow Spring's default error handler to work properly
                    .anyRequest()
                    .authenticated() // All other endpoints require a valid authentication
            );

    // 5. Critical Step: Insert the custom JWT filter before the default password filter
    http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    // Note: The custom JwtAuthenticationFilter is intentionally omitted here for now
    // so we can test the basic Login API flow in isolation first.

    return http.build();
  }
}
