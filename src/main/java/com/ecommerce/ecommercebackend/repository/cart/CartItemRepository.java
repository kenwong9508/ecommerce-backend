package com.ecommerce.ecommercebackend.repository.cart;

import com.ecommerce.ecommercebackend.model.cart.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Checks if a specific product is already inside the user's cart
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    // Retrieves all items belonging to a specific cart
    List<CartItem> findAllByCartId(Long cartId);
}
