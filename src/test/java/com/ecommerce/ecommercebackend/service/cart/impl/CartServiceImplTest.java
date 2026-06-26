package com.ecommerce.ecommercebackend.service.cart.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ecommerce.ecommercebackend.dto.cart.AddToCartRequest;
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

    // ==========================================
    // Helper Method: Create Update Request
    // ==========================================
    private UpdateCartItemRequest createUpdateRequest(int quantity) {
        return UpdateCartItemRequest.builder().quantity(quantity).build();
    }

    // ==========================================
    // Test Group 2: Update Item Quantity
    // ==========================================
    @Nested
    @DisplayName("Method: updateItemQuantity")
    class UpdateItemQuantityTests {

        @Test
        @DisplayName("Success: Update quantity when user is owner and stock is sufficient")
        void should_UpdateQuantity_When_ValidRequest() {
            // 1. ARRANGE
            Long targetCartItemId = 1001L;
            int sufficientStock = 10;
            int newQuantity = 5;

            Product mockProduct = createMockProduct(101L, sufficientStock);
            CartItem existingItem = CartItem.builder()
                    .id(targetCartItemId)
                    .cart(mockCart) // mockCart belongs to STANDARD_USER_ID (1L)
                    .product(mockProduct)
                    .quantity(2)
                    .unitPrice(mockProduct.getPrice())
                    .build();

            UpdateCartItemRequest request = createUpdateRequest(newQuantity);

            when(cartItemRepository.findById(targetCartItemId)).thenReturn(Optional.of(existingItem));
            when(cartItemRepository.findAllByCartId(STANDARD_CART_ID)).thenReturn(List.of(existingItem));

            // 2. ACT
            CartResponse response = cartService.updateItemQuantity(STANDARD_USER_ID, targetCartItemId, request);

            // 3. ASSERT
            assertNotNull(response);

            // Verify that the new quantity (5) was saved successfully
            ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
            verify(cartItemRepository, times(1)).save(itemCaptor.capture());
            assertEquals(newQuantity, itemCaptor.getValue().getQuantity());

            verify(cartItemRepository, times(1)).flush();
        }

        @Test
        @DisplayName("Fail: Throw exception when user is not the owner (IDOR)")
        void should_ThrowException_When_UserIsNotOwner() {
            // 1. ARRANGE
            Long targetCartItemId = 1001L;
            Long maliciousUserId = 999L; // Hacker / unauthorized user

            Product mockProduct = createMockProduct(101L, 10);
            CartItem existingItem = CartItem.builder()
                    .id(targetCartItemId)
                    .cart(mockCart) // mockCart belongs to STANDARD_USER_ID (1L)
                    .product(mockProduct)
                    .quantity(2)
                    .build();

            UpdateCartItemRequest request = createUpdateRequest(5);

            when(cartItemRepository.findById(targetCartItemId)).thenReturn(Optional.of(existingItem));

            // 2. ACT & ASSERT
            // Pass maliciousUserId to attempt unauthorized modification
            assertThrows(UnauthorizedAccessException.class, () -> {
                cartService.updateItemQuantity(maliciousUserId, targetCartItemId, request);
            });

            // Strict check: Database must NOT be modified under any circumstances
            verify(cartItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Fail: Throw exception when requested quantity exceeds stock")
        void should_ThrowException_When_UpdateExceedsStock() {
            // 1. ARRANGE
            Long targetCartItemId = 1001L;
            int lowStock = 3;
            int excessiveQuantity = 5;

            Product mockProduct = createMockProduct(101L, lowStock);
            CartItem existingItem = CartItem.builder()
                    .id(targetCartItemId)
                    .cart(mockCart)
                    .product(mockProduct)
                    .quantity(2)
                    .build();

            UpdateCartItemRequest request = createUpdateRequest(excessiveQuantity);

            when(cartItemRepository.findById(targetCartItemId)).thenReturn(Optional.of(existingItem));

            // 2. ACT & ASSERT
            assertThrows(InsufficientStockException.class, () -> {
                cartService.updateItemQuantity(STANDARD_USER_ID, targetCartItemId, request);
            });

            // Strict check: Database must NOT be modified
            verify(cartItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Fail: Throw exception when cart item does not exist")
        void should_ThrowException_When_CartItemNotFound() {
            // 1. ARRANGE
            Long invalidCartItemId = 9999L;
            UpdateCartItemRequest request = createUpdateRequest(5);

            when(cartItemRepository.findById(invalidCartItemId)).thenReturn(Optional.empty());

            // 2. ACT & ASSERT
            assertThrows(ResourceNotFoundException.class, () -> {
                cartService.updateItemQuantity(STANDARD_USER_ID, invalidCartItemId, request);
            });

            verify(cartItemRepository, never()).save(any());
        }
    }
}
