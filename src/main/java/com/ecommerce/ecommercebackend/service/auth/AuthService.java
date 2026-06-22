package com.ecommerce.ecommercebackend.service.auth;

import com.ecommerce.ecommercebackend.dto.auth.AuthResponse;
import com.ecommerce.ecommercebackend.dto.auth.LoginRequest;
import com.ecommerce.ecommercebackend.dto.auth.RegisterRequest;
import com.ecommerce.ecommercebackend.model.user.User;

public interface AuthService {

    // Defines the contract for user registration
    User registerUser(RegisterRequest request);

    AuthResponse login(LoginRequest loginRequest);
}
