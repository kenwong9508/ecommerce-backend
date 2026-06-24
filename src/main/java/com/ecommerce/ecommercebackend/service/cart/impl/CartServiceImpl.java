package com.ecommerce.ecommercebackend.service.cart.impl;

import com.ecommerce.ecommercebackend.dto.cart.AddToCartRequest;
import com.ecommerce.ecommercebackend.dto.cart.CartItemResponse;
import com.ecommerce.ecommercebackend.dto.cart.CartResponse;
import com.ecommerce.ecommercebackend.exception.InsufficientStockException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
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
