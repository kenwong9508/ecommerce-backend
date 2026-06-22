package com.ecommerce.ecommercebackend.service.auth;

import com.ecommerce.ecommercebackend.dto.auth.AuthResponse;
import com.ecommerce.ecommercebackend.dto.auth.LoginRequest;
import com.ecommerce.ecommercebackend.dto.auth.RefreshTokenRequest;
import com.ecommerce.ecommercebackend.dto.auth.RegisterRequest;
import com.ecommerce.ecommercebackend.model.user.User;

public interface AuthService {

    /**
     * Registers a new user into the system.
     */
    User registerUser(RegisterRequest request);

    /**
     * Authenticates a user and generates access/refresh tokens.
     */
    AuthResponse login(LoginRequest loginRequest);

    /**
     * Validates an old refresh token and issues new access/refresh tokens.
     */
    AuthResponse renewAuthTokens(RefreshTokenRequest request);
}
