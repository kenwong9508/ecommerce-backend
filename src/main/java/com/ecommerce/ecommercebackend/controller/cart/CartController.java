package com.ecommerce.ecommercebackend.controller.cart;

import com.ecommerce.ecommercebackend.dto.cart.AddToCartRequest;
import com.ecommerce.ecommercebackend.dto.cart.CartResponse;
import com.ecommerce.ecommercebackend.dto.common.ApiResponse;
import com.ecommerce.ecommercebackend.security.CustomUserDetails;
import com.ecommerce.ecommercebackend.service.cart.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItemToCart(
            @AuthenticationPrincipal CustomUserDetails userDetails, @Valid @RequestBody AddToCartRequest request) {

        // Execute business logic and retrieve the fully constructed response
        CartResponse updatedCart = cartService.addItemToCart(userDetails.getId(), request);

        // Wrap the response in the unified ApiResponse format
        return ResponseEntity.ok(ApiResponse.success(updatedCart));
    }
}
