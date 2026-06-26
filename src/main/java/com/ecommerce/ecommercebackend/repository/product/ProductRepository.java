package com.ecommerce.ecommercebackend.repository.product;

import com.ecommerce.ecommercebackend.model.product.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Custom query method to find all products that belong to a specific category ID
    List<Product> findByCategoryId(Long categoryId);
}
