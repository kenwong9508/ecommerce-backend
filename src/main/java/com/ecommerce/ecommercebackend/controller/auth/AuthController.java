package com.ecommerce.ecommercebackend.controller.auth;

import com.ecommerce.ecommercebackend.dto.auth.AuthResponse;
import com.ecommerce.ecommercebackend.dto.auth.LoginRequest;
import com.ecommerce.ecommercebackend.dto.auth.RegisterRequest;
import com.ecommerce.ecommercebackend.dto.common.ApiResponse;
import com.ecommerce.ecommercebackend.dto.user.UserResponse;
import com.ecommerce.ecommercebackend.model.user.User;
import com.ecommerce.ecommercebackend.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth") // Base URL for all authentication endpoints
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<UserResponse>> registerUser(
      @Valid @RequestBody RegisterRequest request) { // @Valid triggers the DTO validations

    log.info("Received registration request for email: {}", request.getEmail());

    // 1. Pass the validated DTO to the Service layer
    User savedUser = authService.registerUser(request);

    // 2. Convert the saved Database Entity into a safe DTO
    UserResponse responseDto = UserResponse.fromEntity(savedUser);

    // 3. Wrap the DTO in our standard ApiResponse and return 201 Created
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(responseDto));
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(
      @Valid @RequestBody LoginRequest loginRequest) {

    AuthResponse authResponse = authService.login(loginRequest);

    return ResponseEntity.ok(ApiResponse.success(authResponse));
  }
}
