package com.ecommerce.ecommercebackend.service.auth.impl;

import com.ecommerce.ecommercebackend.dto.auth.RegisterRequest;
import com.ecommerce.ecommercebackend.model.user.Role;
import com.ecommerce.ecommercebackend.model.user.User;
import com.ecommerce.ecommercebackend.repository.user.RoleRepository;
import com.ecommerce.ecommercebackend.repository.user.UserRepository;
import com.ecommerce.ecommercebackend.service.auth.AuthService;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service // Marks this class as a Spring Service component containing business logic
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;

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
    Role defaultRole =
        roleRepository
            .findByName("USER")
            .orElseThrow(
                () -> new RuntimeException("Error: Default role not found. Did the Seeder run?"));

    // Assign the default role to the newly created user
    user.setRoles(Collections.singleton(defaultRole));

    // 5. Persist the user to the database
    User savedUser = userRepository.save(user);

    log.info("✅ Successfully registered new user: {}", savedUser.getUsername());

    return savedUser;
  }
}
