package com.ecommerce.ecommercebackend.config;

import com.ecommerce.ecommercebackend.model.user.Role;
import com.ecommerce.ecommercebackend.repository.user.RoleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

  private final RoleRepository roleRepository;

  @Override
  public void run(String... args) throws Exception {

    // Define all the default roles your system needs
    List<String> defaultRoles = List.of("USER", "ADMIN", "MANAGER", "SUPER_ADMIN");

    // Loop through the list and insert them if they don't exist
    for (String roleName : defaultRoles) {
      if (roleRepository.findByName(roleName).isEmpty()) {
        Role role = new Role();
        role.setName(roleName);
        roleRepository.save(role);

        // Best Practice: Use '{}' for variable substitution in SLF4J
        log.info("✅ DatabaseSeeder: Inserted default role -> {}", roleName);
      }
    }
  }
}
