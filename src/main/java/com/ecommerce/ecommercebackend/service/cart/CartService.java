package com.ecommerce.ecommercebackend.service.cart;

import com.ecommerce.ecommercebackend.dto.cart.AddToCartRequest;
import com.ecommerce.ecommercebackend.dto.cart.CartResponse;
import com.ecommerce.ecommercebackend.dto.cart.UpdateCartItemRequest;

public interface CartService {

    // Processes the request and returns the updated cart state
    CartResponse addItemToCart(Long userId, AddToCartRequest request);

    // update existing item quantity
    CartResponse updateItemQuantity(Long userId, Long cartItemId, UpdateCartItemRequest request);

    // Retrieve the user's cart
    CartResponse getCart(Long userId);
}
