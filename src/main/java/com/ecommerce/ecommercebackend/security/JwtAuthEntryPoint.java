package com.ecommerce.ecommercebackend.security;

import com.ecommerce.ecommercebackend.dto.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Custom entry point that handles unauthorized requests. Returns a standardized JSON response
 * instead of Spring's default HTML error page.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {

        log.warn("Unauthorized access attempt to {}: {}", request.getRequestURI(), authException.getMessage());

        // Set response content type to JSON and status to 401 Unauthorized
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());

        // Construct the standardized API error response
        ApiResponse<Void> apiResponse =
                ApiResponse.error("UNAUTHORIZED", "Valid Access Token is required to access this endpoint.");

        // Write the JSON response directly to the output stream
        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }
}
