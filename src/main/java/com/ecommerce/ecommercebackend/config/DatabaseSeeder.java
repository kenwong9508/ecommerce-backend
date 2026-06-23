package com.ecommerce.ecommercebackend.config;

import com.ecommerce.ecommercebackend.model.product.Category;
import com.ecommerce.ecommercebackend.model.product.Product;
import com.ecommerce.ecommercebackend.model.user.Role;
import com.ecommerce.ecommercebackend.repository.product.CategoryRepository;
import com.ecommerce.ecommercebackend.repository.product.ProductRepository;
import com.ecommerce.ecommercebackend.repository.user.RoleRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedRoles();
        seedCategoriesAndProducts();
    }

    private void seedRoles() {
        List<String> defaultRoles = List.of("USER", "ADMIN", "MANAGER", "SUPER_ADMIN");

        for (String roleName : defaultRoles) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
                log.info("✅ DatabaseSeeder: Inserted default role -> {}", roleName);
            }
        }
    }

    private void seedCategoriesAndProducts() {
        if (categoryRepository.count() == 0) {
            log.info("🌱 DatabaseSeeder: Starting to seed Categories and Products with HKD pricing...");

            // 1. Define local records as temporary data structures for clean data layouts
            record ProductSeed(String name, String description, BigDecimal price, int stock) {}
            record CategorySeed(String name, String description, List<ProductSeed> products) {}

            // 2. Hardcoded Data Block - Organized for maximum readability ("一眼睇哂")
            List<CategorySeed> categorySeeds = List.of(
                    new CategorySeed(
                            "Electronics",
                            "Latest flagship smartphones, high-performance laptops, and premium gadgets.",
                            List.of(
                                    new ProductSeed(
                                            "iPhone 17 Pro Max (256GB)",
                                            "The ultimate flagship featuring the A19 Pro chip, stunning display, and Apple Intelligence.",
                                            new BigDecimal("10199.00"),
                                            50),
                                    new ProductSeed(
                                            "MacBook Pro 16-inch (M4 Max)",
                                            "Unrivaled pro workflow machine powered by M4 Max silicon with 36GB unified memory.",
                                            new BigDecimal("27999.00"),
                                            20))),
                    new CategorySeed(
                            "Gaming",
                            "Top-tier video games for next-gen consoles and PC.",
                            List.of(
                                    new ProductSeed(
                                            "Resident Evil 9",
                                            "The thrilling next chapter in the legendary survival horror franchise. Face new nightmares.",
                                            new BigDecimal("498.00"),
                                            100),
                                    new ProductSeed(
                                            "Pokémon Legends: Z-A",
                                            "A new adventure set entirely within Lumiose City. Catch and battle in a vibrant world.",
                                            new BigDecimal("428.00"),
                                            150),
                                    new ProductSeed(
                                            "Grand Theft Auto VI",
                                            "Welcome to Leonida. Experience the most anticipated open-world masterpiece of the decade.",
                                            new BigDecimal("648.00"),
                                            300))),
                    new CategorySeed(
                            "Monitors",
                            "High-resolution desktop displays for professional gaming, productivity, and color grading.",
                            List.of(
                                    new ProductSeed(
                                            "LG UltraGear 27-inch OLED Gaming Monitor",
                                            "240Hz refresh rate with 0.03ms response time for absolute competitive gaming advantage.",
                                            new BigDecimal("6490.00"),
                                            35),
                                    new ProductSeed(
                                            "Dell UltraSharp 32-inch 4K USB-C Hub Monitor",
                                            "Exceptional color accuracy with IPS Black technology, 4K resolution, and 90W power delivery.",
                                            new BigDecimal("7299.00"),
                                            25))));

            // 3. Execution Block - Nested loops to process and save data automatically
            for (CategorySeed catSeed : categorySeeds) {
                // Save the Category first to get its generated ID
                Category category = Category.builder()
                        .name(catSeed.name())
                        .description(catSeed.description())
                        .build();
                Category savedCategory = categoryRepository.save(category);

                // Loop through and save all products associated with this category
                for (ProductSeed prodSeed : catSeed.products()) {
                    Product product = Product.builder()
                            .name(prodSeed.name())
                            .description(prodSeed.description())
                            .price(prodSeed.price())
                            .stockQuantity(prodSeed.stock())
                            .category(savedCategory) // Link foreign key
                            .build();
                    productRepository.save(product);
                }
                log.info(
                        "✅ DatabaseSeeder: Seeded category '{}' with {} products.",
                        savedCategory.getName(),
                        catSeed.products().size());
            }

            log.info("✅ DatabaseSeeder: All categories and products seeded successfully with HKD prices!");
        } else {
            log.info("⚡ DatabaseSeeder: Categories and Products already exist. Skipping seed.");
        }
    }
}
