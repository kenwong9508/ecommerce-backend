package com.ecommerce.ecommercebackend.controller.cart;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.ecommercebackend.dto.cart.AddToCartRequest;
import com.ecommerce.ecommercebackend.dto.cart.UpdateCartItemRequest;
import com.ecommerce.ecommercebackend.model.cart.Cart;
import com.ecommerce.ecommercebackend.model.cart.CartItem;
import com.ecommerce.ecommercebackend.model.product.Category;
import com.ecommerce.ecommercebackend.model.product.Product;
import com.ecommerce.ecommercebackend.model.user.User;
import com.ecommerce.ecommercebackend.repository.cart.CartItemRepository;
import com.ecommerce.ecommercebackend.repository.cart.CartRepository;
import com.ecommerce.ecommercebackend.repository.product.CategoryRepository;
import com.ecommerce.ecommercebackend.repository.product.ProductRepository;
import com.ecommerce.ecommercebackend.repository.user.UserRepository;
import com.ecommerce.ecommercebackend.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 🌟 Rollback database automatically after each test
@ActiveProfiles("test") // test environment
@DisplayName("🛒 Cart Controller Integration Tests")
class CartControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc; // Our internal Postman

    @Autowired
    private ObjectMapper objectMapper; // Converts Objects to JSON

    // Real Repositories to setup database state
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User testUser;
    private Product testProduct;
    private Authentication testAuth;

    @BeforeEach
    void setUp() {
        // 1. Insert REAL User into Database
        testUser = User.builder()
                .username("integration_test_user")
                .email("test@integration.com")
                .password("hashed_password")
                .build();
        userRepository.save(testUser);

        // 2. 🌟 Security Bypass: Inject mock user into SecurityContext
        testAuth = mockUserLogin(testUser);

        // 3. Insert REAL Category into Database
        Category testCategory = Category.builder()
                .name("Electronics")
                .description("Description")
                .build();
        categoryRepository.save(testCategory);

        // 4. Insert REAL Product into Database
        testProduct = Product.builder()
                .name("Test Cyberpunk Keyboard")
                .price(BigDecimal.valueOf(500.00))
                .stockQuantity(100)
                .category(testCategory)
                .build();
        productRepository.save(testProduct);
    }
    // =========================================================================
    // 1️⃣ API Endpoint: POST /api/v1/cart/items (Add Item)
    // =========================================================================
    @Nested
    @DisplayName("API: POST /api/v1/cart/items")
    class AddItemToCartTests {

        @Test
        @DisplayName("Success: Return 200 and add item to database when request is valid")
        void should_Return200AndSaveToDb_When_RequestIsValid() throws Exception {
            int requestQuantity = 2;
            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(testProduct.getId())
                    .quantity(requestQuantity)
                    .build();

            mockMvc.perform(post("/api/v1/cart/items")
                            .with(authentication(testAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.items[0].productId").value(testProduct.getId()))
                    .andExpect(jsonPath("$.data.items[0].quantity").value(requestQuantity))
                    .andExpect(jsonPath("$.data.totalPrice").value(1000.00))
                    .andExpect(jsonPath("$.error").isEmpty());

            long cartItemCount = cartItemRepository.count();
            assertEquals(1, cartItemCount, "Exactly 1 item should be saved in the database");
        }

        @Test
        @DisplayName("Fail: Return 400 Bad Request when quantity is invalid")
        void should_Return400_When_QuantityIsInvalid() throws Exception {
            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(testProduct.getId())
                    .quantity(0) // Invalid boundary value
                    .build();

            mockMvc.perform(post("/api/v1/cart/items")
                            .with(authentication(testAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.error").exists());

            long cartItemCount = cartItemRepository.count();
            assertEquals(0, cartItemCount, "Database must remain untouched on validation failure");
        }

        @Test
        @DisplayName("Fail: Return 404 Not Found when product does not exist")
        void should_Return404_When_ProductNotFound() throws Exception {
            Long nonExistentProductId = 9999L;
            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(nonExistentProductId)
                    .quantity(1)
                    .build();

            mockMvc.perform(post("/api/v1/cart/items")
                            .with(authentication(testAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.error").exists());

            long cartItemCount = cartItemRepository.count();
            assertEquals(0, cartItemCount, "Database must remain untouched");
        }
    }

    // =========================================================================
    // 2️⃣ API Endpoint: PUT /api/v1/cart/items/{cartItemId} (Update Quantity)
    // =========================================================================
    @Nested
    @DisplayName("API: PUT /api/v1/cart/items/{cartItemId}")
    class UpdateItemQuantityTests {

        @Test
        @DisplayName("Success: Return 200 and update quantity in DB when request is valid")
        void should_Return200AndUpdateDb_When_RequestIsValid() throws Exception {
            // 1. ARRANGE
            // First, create a Cart for the test user
            Cart testCart = new Cart();
            testCart.setUser(testUser);
            testCart = cartRepository.save(testCart);

            // Then, insert a real CartItem linked to the Cart and Product
            int initialQuantity = 2;
            CartItem existingItem = CartItem.builder()
                    .cart(testCart)
                    .product(testProduct)
                    .quantity(initialQuantity)
                    .unitPrice(testProduct.getPrice())
                    .build();
            existingItem = cartItemRepository.save(existingItem);

            // Prepare the PUT request body
            int newQuantity = 5;
            UpdateCartItemRequest request = new UpdateCartItemRequest();
            request.setQuantity(newQuantity);

            // 2. ACT & ASSERT (API Level)
            mockMvc.perform(put("/api/v1/cart/items/" + existingItem.getId())
                            .with(authentication(testAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").exists()) // Data object should be populated
                    .andExpect(jsonPath("$.error").isEmpty()); // Error object should be present but null

            // 3. ASSERT (Database Level)
            CartItem updatedItem =
                    cartItemRepository.findById(existingItem.getId()).orElseThrow();
            assertEquals(newQuantity, updatedItem.getQuantity(), "Database quantity should be updated to 5");
        }

        @Test
        @DisplayName("Fail: Return 400 Bad Request and do not update DB when quantity is invalid")
        void should_Return400AndNotUpdateDb_When_QuantityIsInvalid() throws Exception {
            // 1. ARRANGE
            Cart testCart = new Cart();
            testCart.setUser(testUser);
            cartRepository.save(testCart);

            int initialQuantity = 2;
            CartItem existingItem = CartItem.builder()
                    .cart(testCart)
                    .product(testProduct)
                    .quantity(initialQuantity)
                    .unitPrice(testProduct.getPrice())
                    .build();
            existingItem = cartItemRepository.save(existingItem);

            // Prepare an invalid PUT request body (quantity = 0)
            UpdateCartItemRequest request = new UpdateCartItemRequest();
            request.setQuantity(0);

            // 2. ACT & ASSERT (API Level)
            mockMvc.perform(put("/api/v1/cart/items/" + existingItem.getId())
                            .with(authentication(testAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists()) // Error object should be populated
                    .andExpect(jsonPath("$.data").isEmpty()); // Data object should be present but null

            // 3. ASSERT (Database Level) - Strictly ensure DB is unchanged
            CartItem untouchedItem =
                    cartItemRepository.findById(existingItem.getId()).orElseThrow();
            assertEquals(initialQuantity, untouchedItem.getQuantity(), "Database quantity must remain unchanged");
        }

        @Test
        @DisplayName("Fail: Return 404 Not Found when cart item does not exist")
        void should_Return404_When_CartItemNotFound() throws Exception {
            // 1. ARRANGE
            Long nonExistentCartItemId = 9999L;
            UpdateCartItemRequest request = new UpdateCartItemRequest();
            request.setQuantity(5);

            // 2. ACT & ASSERT
            mockMvc.perform(put("/api/v1/cart/items/" + nonExistentCartItemId)
                            .with(authentication(testAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").exists()) // Error object should be populated
                    .andExpect(jsonPath("$.data").isEmpty()); // Data object should be present but null
        }
    }

    // =========================================================================
    // 3️⃣ API Endpoint: GET /api/v1/cart (Get Cart)
    // =========================================================================
    @Nested
    @DisplayName("API: GET /api/v1/cart")
    class GetCartTests {

        @Test
        @DisplayName("Success: Return 200 and correct cart details when cart has items")
        void should_Return200AndCartDetails_When_CartHasItems() throws Exception {
            // 1. ARRANGE
            // Create a Cart for testUser
            Cart testCart = new Cart();
            testCart.setUser(testUser);
            testCart = cartRepository.save(testCart);

            // Create a second product to test multiple items in cart
            Product secondProduct = Product.builder()
                    .name("Test Neon Mouse")
                    .price(BigDecimal.valueOf(100.00))
                    .stockQuantity(50)
                    .category(testProduct.getCategory()) // Reuse the same category
                    .build();
            secondProduct = productRepository.save(secondProduct);

            // Insert 2 items into the database cart
            // Item 1: 1x Cyberpunk Keyboard (500.00)
            CartItem item1 = CartItem.builder()
                    .cart(testCart)
                    .product(testProduct)
                    .quantity(1)
                    .unitPrice(testProduct.getPrice())
                    .build();
            cartItemRepository.save(item1);

            // Item 2: 2x Neon Mouse (100.00 * 2 = 200.00)
            CartItem item2 = CartItem.builder()
                    .cart(testCart)
                    .product(secondProduct)
                    .quantity(2)
                    .unitPrice(secondProduct.getPrice())
                    .build();
            cartItemRepository.save(item2);

            // Total price should be: 500.00 + 200.00 = 700.00

            // 2. ACT & ASSERT
            mockMvc.perform(get("/api/v1/cart")
                            .with(authentication(testAuth)) // Authenticated as testUser
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").isEmpty()) // Error field must be present but null
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.cartId").value(testCart.getId()))

                    // Verify there are exactly 2 items in the array
                    .andExpect(jsonPath("$.data.items", hasSize(2)))

                    // Verify the math logic on total price
                    .andExpect(jsonPath("$.data.totalPrice").value(700.00));
        }

        @Test
        @DisplayName("Success: Return 200 and empty items list when new user fetches cart")
        void should_Return200AndEmptyCart_When_UserHasNoCartItems() throws Exception {
            // 1. ARRANGE: Create a completely clean new user with no cart history
            User emptyUser = User.builder()
                    .username("empty_cart_user")
                    .email("empty@integration.com")
                    .password("hashed_password")
                    .build();
            emptyUser = userRepository.save(emptyUser);

            // Generate token for this specific new user
            Authentication emptyAuth = mockUserLogin(emptyUser);

            // 2. ACT & ASSERT
            mockMvc.perform(get("/api/v1/cart")
                            .with(authentication(emptyAuth)) // Use the new empty user's credentials
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").isEmpty())
                    .andExpect(jsonPath("$.data").exists())

                    // Empty cart should return an empty array [] and 0 total price
                    .andExpect(jsonPath("$.data.items", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalPrice").value(0.00));
        }
    }

    // =========================================================================
    // 4️⃣ API Endpoint: DELETE /api/v1/cart/items/{cartItemId} (Remove Item)
    // =========================================================================
    @Nested
    @DisplayName("API: DELETE /api/v1/cart/items/{cartItemId}")
    class RemoveItemTests {

        @Test
        @DisplayName("Success: Return 200 and remove item from DB when request is valid")
        void should_Return200AndRemoveFromDb_When_RequestIsValid() throws Exception {
            // 1. ARRANGE
            // Create a Cart for testUser
            Cart testCart = new Cart();
            testCart.setUser(testUser);
            testCart = cartRepository.save(testCart);

            // Insert a real CartItem into the database
            CartItem itemToDelete = CartItem.builder()
                    .cart(testCart)
                    .product(testProduct)
                    .quantity(1)
                    .unitPrice(testProduct.getPrice())
                    .build();
            itemToDelete = cartItemRepository.save(itemToDelete);

            // 2. ACT & ASSERT (API Level)
            mockMvc.perform(delete("/api/v1/cart/items/" + itemToDelete.getId())
                            .with(authentication(testAuth))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").isEmpty()); // Error object should be null on success

            // 3. ASSERT (Database Level) - Strictly ensure it's deleted
            boolean itemStillExists = cartItemRepository.existsById(itemToDelete.getId());
            assertFalse(itemStillExists, "CartItem should be completely removed from the database");
        }

        @Test
        @DisplayName("Fail: Return 404 Not Found when trying to delete non-existent item")
        void should_Return404_When_CartItemDoesNotExist() throws Exception {
            // 1. ARRANGE
            Long nonExistentCartItemId = 9999L;

            // 2. ACT & ASSERT
            mockMvc.perform(delete("/api/v1/cart/items/" + nonExistentCartItemId)
                            .with(authentication(testAuth))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound()) // Assuming it throws ResourceNotFoundException
                    .andExpect(jsonPath("$.error").exists()) // Error object must be populated
                    .andExpect(jsonPath("$.data").isEmpty()); // Data object must be null
        }
    }

    // ==========================================
    // Helper Method: Security Bypass
    // ==========================================
    private Authentication mockUserLogin(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(
                user.getId(), user.getEmail(), user.getUsername(), user.getPassword(), java.util.List.of());

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
