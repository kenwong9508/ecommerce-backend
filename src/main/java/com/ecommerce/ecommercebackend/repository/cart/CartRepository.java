package com.ecommerce.ecommercebackend.repository.cart;

import com.ecommerce.ecommercebackend.model.cart.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Retrieves the active cart for a specific user
    Optional<Cart> findByUserId(Long userId);
}
