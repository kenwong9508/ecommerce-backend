package com.ecommerce.ecommercebackend.service.cart;

import com.ecommerce.ecommercebackend.dto.cart.AddToCartRequest;
import com.ecommerce.ecommercebackend.dto.cart.CartResponse;

public interface CartService {

    // Processes the request and returns the updated cart state
    CartResponse addItemToCart(Long userId, AddToCartRequest request);
}
