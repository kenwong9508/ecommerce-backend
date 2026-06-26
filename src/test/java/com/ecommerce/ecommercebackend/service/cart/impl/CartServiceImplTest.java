package com.ecommerce.ecommercebackend.service.cart.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ecommerce.ecommercebackend.dto.cart.AddToCartRequest;
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
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cart Service Unit Tests")
class CartServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    // ==========================================
    // Shared Test Data & Constants
    // ==========================================
    private User mockUser;
    private Cart mockCart;
    private static final Long STANDARD_USER_ID = 1L;
    private static final Long STANDARD_CART_ID = 50L;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(STANDARD_USER_ID).email("test@test.com").build();
        mockCart = Cart.builder().id(STANDARD_CART_ID).user(mockUser).build();
    }

    // ==========================================
    // Helper Methods (Eliminate Magic Numbers)
    // ==========================================
    private Product createMockProduct(Long id, int stockQuantity) {
        return Product.builder()
                .id(id)
                .name("Test Product " + id)
                .price(BigDecimal.valueOf(100.00))
                .stockQuantity(stockQuantity)
                .build();
    }

    private AddToCartRequest createAddRequest(Long productId, int quantity) {
        return AddToCartRequest.builder()
                .productId(productId)
                .quantity(quantity)
                .build();
    }

    // ==========================================
    // Test Group: Add Item To Cart
    // ==========================================
    @Nested
    @DisplayName("Method: addItemToCart")
    class AddItemToCartTests {

        @Test
        @DisplayName("Success: Add a completely new product to the cart")
        void should_SaveNewCartItem_When_ProductNotInCart() {
            // 1. ARRANGE
            Long targetProductId = 101L;
            int sufficientStock = 10;
            int requestedQuantity = 2;

            Product mockProduct = createMockProduct(targetProductId, sufficientStock);
            AddToCartRequest request = createAddRequest(targetProductId, requestedQuantity);

            when(userRepository.findById(STANDARD_USER_ID)).thenReturn(Optional.of(mockUser));
            when(cartRepository.findByUserId(STANDARD_USER_ID)).thenReturn(Optional.of(mockCart));
            when(productRepository.findById(targetProductId)).thenReturn(Optional.of(mockProduct));
            // Mock: Product is not yet in the cart
            when(cartItemRepository.findByCartIdAndProductId(STANDARD_CART_ID, targetProductId))
                    .thenReturn(Optional.empty());

            CartItem savedItem = CartItem.builder()
                    .id(1001L)
                    .cart(mockCart)
                    .product(mockProduct)
                    .quantity(requestedQuantity)
                    .unitPrice(mockProduct.getPrice())
                    .build();
            when(cartItemRepository.findAllByCartId(STANDARD_CART_ID)).thenReturn(List.of(savedItem));

            // 2. ACT
            CartResponse response = cartService.addItemToCart(STANDARD_USER_ID, request);

            // 3. ASSERT
            assertNotNull(response);
            ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
            verify(cartItemRepository, times(1)).save(itemCaptor.capture());
            // Verify exactly 2 items were saved
            assertEquals(requestedQuantity, itemCaptor.getValue().getQuantity());
        }

        @Test
        @DisplayName("Success: Merge quantity when product is already in the cart")
        void should_UpdateExistingItemQuantity_When_ProductAlreadyInCart() {
            // 1. ARRANGE
            Long targetProductId = 101L;
            int sufficientStock = 10;
            int existingQuantity = 3;
            int newlyRequestedQuantity = 2; // 3 + 2 = 5 (No overselling)

            Product mockProduct = createMockProduct(targetProductId, sufficientStock);
            AddToCartRequest request = createAddRequest(targetProductId, newlyRequestedQuantity);

            CartItem existingItem = CartItem.builder()
                    .id(1001L)
                    .cart(mockCart)
                    .product(mockProduct)
                    .quantity(existingQuantity)
                    .unitPrice(mockProduct.getPrice())
                    .build();

            when(userRepository.findById(STANDARD_USER_ID)).thenReturn(Optional.of(mockUser));
            when(cartRepository.findByUserId(STANDARD_USER_ID)).thenReturn(Optional.of(mockCart));
            when(productRepository.findById(targetProductId)).thenReturn(Optional.of(mockProduct));
            // Mock: Cart already has 3 items of this product
            when(cartItemRepository.findByCartIdAndProductId(STANDARD_CART_ID, targetProductId))
                    .thenReturn(Optional.of(existingItem));

            when(cartItemRepository.findAllByCartId(STANDARD_CART_ID)).thenReturn(List.of(existingItem));

            // 2. ACT
            cartService.addItemToCart(STANDARD_USER_ID, request);

            // 3. ASSERT
            ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
            verify(cartItemRepository, times(1)).save(itemCaptor.capture());
            // Verify the quantity was updated to 5 (existingQuantity: 3 + newlyRequestedQuantity: 2)
            assertEquals(
                    existingQuantity + newlyRequestedQuantity,
                    itemCaptor.getValue().getQuantity());
        }

        @Test
        @DisplayName("Fail: Throw exception when stock is insufficient for a new item")
        void should_ThrowException_When_StockIsInsufficientForNewItem() {
            // 1. ARRANGE
            Long targetProductId = 101L;
            int lowStock = 1;
            int excessiveQuantity = 5;

            Product mockProduct = createMockProduct(targetProductId, lowStock);
            AddToCartRequest request = createAddRequest(targetProductId, excessiveQuantity);

            when(userRepository.findById(STANDARD_USER_ID)).thenReturn(Optional.of(mockUser));
            when(cartRepository.findByUserId(STANDARD_USER_ID)).thenReturn(Optional.of(mockCart));
            when(productRepository.findById(targetProductId)).thenReturn(Optional.of(mockProduct));

            // 2. ACT & ASSERT
            assertThrows(InsufficientStockException.class, () -> {
                cartService.addItemToCart(STANDARD_USER_ID, request);
            });
            // Strictly ensure database is not modified
            verify(cartItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Fail: Throw exception when merged quantity exceeds total stock")
        void should_ThrowException_When_MergedQuantityExceedsStock() {
            // 1. ARRANGE
            Long targetProductId = 101L;
            int totalStock = 5;
            int existingQuantity = 4;
            int newlyRequestedQuantity = 2; // 4 + 2 = 6 (Exceeds stock!)

            Product mockProduct = createMockProduct(targetProductId, totalStock);
            AddToCartRequest request = createAddRequest(targetProductId, newlyRequestedQuantity);

            CartItem existingItem = CartItem.builder()
                    .id(1001L)
                    .cart(mockCart)
                    .product(mockProduct)
                    .quantity(existingQuantity)
                    .build();

            when(userRepository.findById(STANDARD_USER_ID)).thenReturn(Optional.of(mockUser));
            when(cartRepository.findByUserId(STANDARD_USER_ID)).thenReturn(Optional.of(mockCart));
            when(productRepository.findById(targetProductId)).thenReturn(Optional.of(mockProduct));
            when(cartItemRepository.findByCartIdAndProductId(STANDARD_CART_ID, targetProductId))
                    .thenReturn(Optional.of(existingItem));

            // 2. ACT & ASSERT
            assertThrows(InsufficientStockException.class, () -> {
                cartService.addItemToCart(STANDARD_USER_ID, request);
            });
            // Strictly ensure database is not modified
            verify(cartItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Fail: Throw exception when user is not found")
        void should_ThrowException_When_UserNotFound() {
            // 1. ARRANGE
            Long invalidUserId = 9999L;
            AddToCartRequest request = createAddRequest(101L, 1);

            // Mock: User not found in DB
            when(userRepository.findById(invalidUserId)).thenReturn(Optional.empty());

            // 2. ACT & ASSERT
            assertThrows(ResourceNotFoundException.class, () -> {
                cartService.addItemToCart(invalidUserId, request);
            });

            // Ensure process stops immediately without querying Product
            verify(productRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Fail: Throw exception when product is not found")
        void should_ThrowException_When_ProductNotFound() {
            // 1. ARRANGE
            Long invalidProductId = 9999L;
            AddToCartRequest request = createAddRequest(invalidProductId, 1);

            when(userRepository.findById(STANDARD_USER_ID)).thenReturn(Optional.of(mockUser));
            when(cartRepository.findByUserId(STANDARD_USER_ID)).thenReturn(Optional.of(mockCart));
            // Mock: Product not found in DB
            when(productRepository.findById(invalidProductId)).thenReturn(Optional.empty());

            // 2. ACT & ASSERT
            assertThrows(ResourceNotFoundException.class, () -> {
                cartService.addItemToCart(STANDARD_USER_ID, request);
            });
            verify(cartItemRepository, never()).save(any());
        }
    }
}
