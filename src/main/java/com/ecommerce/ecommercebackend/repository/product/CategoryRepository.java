package com.ecommerce.ecommercebackend.repository.product;

import com.ecommerce.ecommercebackend.model.product.Category;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Custom query method to find a category by its exact name (useful for future features)
    Optional<Category> findByName(String name);
}
