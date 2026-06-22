package com.ecommerce.ecommercebackend.service.auth.impl;

import com.ecommerce.ecommercebackend.dto.auth.AuthResponse;
import com.ecommerce.ecommercebackend.dto.auth.LoginRequest;
import com.ecommerce.ecommercebackend.dto.auth.RegisterRequest;
import com.ecommerce.ecommercebackend.model.user.Role;
import com.ecommerce.ecommercebackend.model.user.User;
import com.ecommerce.ecommercebackend.repository.user.RoleRepository;
import com.ecommerce.ecommercebackend.repository.user.UserRepository;
import com.ecommerce.ecommercebackend.security.CustomUserDetails;
import com.ecommerce.ecommercebackend.security.JwtTokenProvider;
import com.ecommerce.ecommercebackend.service.auth.AuthService;
import com.ecommerce.ecommercebackend.service.auth.RefreshTokenService;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service // Marks this class as a Spring Service component containing business logic
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    // Utility for hashing passwords securely
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public User registerUser(RegisterRequest request) {

        // 1. Check if the username or email is already taken in the database
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        // 2. Create a new User entity and populate its basic properties
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        // 3. Hash the raw password before saving it to the database
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // 4. Retrieve the default "USER" role from the database
        Role defaultRole = roleRepository
                .findByName("USER")
                .orElseThrow(() -> new RuntimeException("Error: Default role not found. Did the Seeder run?"));

        // Assign the default role to the newly created user
        user.setRoles(Collections.singleton(defaultRole));

        // 5. Persist the user to the database
        User savedUser = userRepository.save(user);

        log.info("✅ Successfully registered new user: {}", savedUser.getUsername());

        return savedUser;
    }

    /**
     * Authenticates the user credentials and generates a new pair of Access and Refresh tokens.
     *
     * @param loginRequest DTO containing the user's email and password.
     * @return AuthResponse containing the generated JWTs.
     */
    @Override
    public AuthResponse login(LoginRequest loginRequest) {

        // 1. Delegate to AuthenticationManager to verify credentials against the database.
        // If credentials are invalid, it automatically throws BadCredentialsException.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        // 2. Authentication successful. Store the authenticated user in the Security Context.
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Extract user details
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // 4. Generate the JWT Access Token and Refresh Token.
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        // 5.  persist the Refresh Token in the database.
        refreshTokenService.saveRefreshTokenEntity(userDetails.getId(), refreshToken);

        // 6. Construct and return the AuthResponse DTO.
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
