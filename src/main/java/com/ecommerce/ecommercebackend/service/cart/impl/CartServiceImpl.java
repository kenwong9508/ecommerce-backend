package com.ecommerce.ecommercebackend.service.cart.impl;

import com.ecommerce.ecommercebackend.dto.cart.AddToCartRequest;
import com.ecommerce.ecommercebackend.dto.cart.CartItemResponse;
import com.ecommerce.ecommercebackend.dto.cart.CartResponse;
import com.ecommerce.ecommercebackend.dto.cart.UpdateCartItemRequest;
import com.ecommerce.ecommercebackend.exception.InsufficientStockException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.exception.UnauthorizedAccessException;
import com.ecommerce.ecommercebackend.model.cart.Cart;
import com.ecommerce.ecommercebackend.model.cart.CartItem;
import com.ecommerce.ecommercebackend.model.product.Product;
import com.ecommerce.ecommercebackend.model.user.User;
import com.ecommerce.ecommercebackend.repository.cart.CartItemRepository;
import com.ecommerce.ecommercebackend.repository.cart.CartRepository;
import com.ecommerce.ecommercebackend.repository.product.ProductRepository;
import com.ecommerce.ecommercebackend.repository.user.UserRepository;
import com.ecommerce.ecommercebackend.service.cart.CartService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public CartResponse addItemToCart(Long userId, AddToCartRequest request) {

        // ✨ Replace RuntimeException with ResourceNotFoundException
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = Cart.builder().user(user).build();
            return cartRepository.save(newCart);
        });

        // ✨ Replace RuntimeException with ResourceNotFoundException
        Product product = productRepository
                .findById(request.getProductId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Product not found with ID: " + request.getProductId()));

        // ✨ Replace IllegalArgumentException with InsufficientStockException
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock. Available stock: " + product.getStockQuantity());
        }

        CartItem existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + request.getQuantity();

            // ✨ Replace IllegalArgumentException with InsufficientStockException
            if (product.getStockQuantity() < newQuantity) {
                throw new InsufficientStockException("Cannot add more. Total cart quantity exceeds available stock.");
            }
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            cartItemRepository.save(newItem);
        }

        cartItemRepository.flush();
        return generateCartResponse(cart.getId());
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long cartItemId, UpdateCartItemRequest request) {

        // 1. Fetch the specific cart item
        CartItem cartItem = cartItemRepository
                .findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with ID: " + cartItemId));

        // 2. CRITICAL SECURITY CHECK (IDOR Protection)
        // Verify that the user attempting to modify this item is the actual owner of the cart.
        // NOTE: Always use .equals() for Long object comparison, not '=='
        Long ownerId = cartItem.getCart().getUser().getId();
        if (!ownerId.equals(userId)) {
            throw new UnauthorizedAccessException("You do not have permission to modify this cart item.");
        }

        // 3. Stock Validation
        Product product = cartItem.getProduct();
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock. Available stock: " + product.getStockQuantity());
        }

        // 4. Update the quantity and save
        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        // 5. Force Hibernate to flush changes to DB before calculating the new totals
        cartItemRepository.flush();

        // 6. Return the fully re-calculated cart state
        return generateCartResponse(cartItem.getCart().getId());
    }

    @Override
    @Transactional(readOnly = true) // 🌟 Optimization: Tells Hibernate to skip dirty-checking for faster reads
    public CartResponse getCart(Long userId) {

        // Find the cart. If found, generate the response.
        // If NOT found, return an empty cart response dynamically without throwing an error.
        return cartRepository
                .findByUserId(userId)
                .map(cart -> generateCartResponse(cart.getId()))
                .orElseGet(() -> CartResponse.builder()
                        .cartId(null) // Or assign a default logic if you prefer
                        .items(List.of()) // Returns an immutable empty list []
                        .totalPrice(BigDecimal.ZERO)
                        .build());
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long cartItemId) {

        // 1. Fetch the specific cart item
        CartItem cartItem = cartItemRepository
                .findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with ID: " + cartItemId));

        // 2. CRITICAL SECURITY CHECK (IDOR Protection)
        // Verify ownership using .equals() on Long objects
        Long ownerId = cartItem.getCart().getUser().getId();
        if (!ownerId.equals(userId)) {
            throw new UnauthorizedAccessException("You do not have permission to modify this cart item.");
        }

        // 3. Save the cartId before deleting the item
        // (Because after deletion, cartItem.getCart() might be inaccessible)
        Long cartId = cartItem.getCart().getId();

        // 4. Delete the item from the database
        cartItemRepository.delete(cartItem);

        // 5. 🔥 The crucial Flush!
        // Force Hibernate to execute the DELETE SQL immediately.
        // If we skip this, generateCartResponse() will query the DB and still see the "deleted" item!
        cartItemRepository.flush();

        // 6. Return the fully re-calculated cart state
        return generateCartResponse(cartId);
    }
    /**
     * Internal helper method to build the comprehensive CartResponse.
     * This logic can be reused for the 'View Cart' endpoint.
     */
    private CartResponse generateCartResponse(Long cartId) {
        List<CartItem> items = cartItemRepository.findAllByCartId(cartId);

        BigDecimal totalPrice = BigDecimal.ZERO;

        List<CartItemResponse> itemResponses = items.stream()
                .map(item -> {
                    // Calculate subtotal for this specific row (quantity * unitPrice)
                    BigDecimal subTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

                    return CartItemResponse.builder()
                            .id(item.getId())
                            .productId(item.getProduct().getId())
                            .productName(item.getProduct().getName())
                            .imageUrl(item.getProduct().getImageUrl())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .subTotal(subTotal)
                            .build();
                })
                .toList();

        // Calculate the grand total
        for (CartItemResponse itemRes : itemResponses) {
            totalPrice = totalPrice.add(itemRes.getSubTotal());
        }

        return CartResponse.builder()
                .cartId(cartId)
                .items(itemResponses)
                .totalPrice(totalPrice)
                .build();
    }
}
