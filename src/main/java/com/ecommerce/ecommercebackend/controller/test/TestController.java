package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.common.ApiResponse;
import com.ecommerce.ecommercebackend.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A test controller to test JWT authentication and route protection. */
@Slf4j
@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    /**
     * A protected endpoint that strictly requires a valid Access Token. Uses @AuthenticationPrincipal
     * to automatically inject the currently authenticated user.
     *
     * @param userDetails The user identity extracted from the JWT and Security Context.
     * @return A success response containing personalized user data.
     */
    @GetMapping("/protected")
    public ResponseEntity<ApiResponse<String>> getProtectedData(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Log the successful entry to the console
        log.info(
                "Protected API accessed successfully by user ID: {}, Email: {}",
                userDetails.getId(),
                userDetails.getUsername());

        // Construct a personalized welcome message using the injected user details
        String welcomeMessage = String.format(
                "Hello! You have successfully passed the security gate. Your User ID is %d and your Email is %s.",
                userDetails.getId(), userDetails.getUsername());

        // Return the message wrapped in your standardized ApiResponse
        return ResponseEntity.ok(ApiResponse.success(welcomeMessage));
    }
}
