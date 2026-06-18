package com.ecommerce.ecommercebackend.repository.user;

import com.ecommerce.ecommercebackend.model.user.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

  // Custom query method to find a role by its exact name
  Optional<Role> findByName(String name);
}
