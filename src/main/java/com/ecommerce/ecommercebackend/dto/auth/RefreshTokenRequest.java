package com.ecommerce.ecommercebackend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object for capturing the refresh token sent by the client.
 */
@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token cannot be blank")
    private String refreshToken;
}
