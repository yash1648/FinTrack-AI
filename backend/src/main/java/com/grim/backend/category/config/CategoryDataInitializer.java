package com.grim.backend.category.config;

import com.grim.backend.category.entity.Category;
import com.grim.backend.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryDataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        List<String> defaultCategories = List.of(
                "Food", "Transportation", "Utilities", "Entertainment",
                "Healthcare", "Education", "Shopping", "Other", "Uncategorized"
        );

        for (String name : defaultCategories) {
            if (categoryRepository.findByNameAndIsDefaultTrue(name).isEmpty()) {
                Category category = Category.builder()
                        .name(name)
                        .isDefault(true)
                        .build();
                categoryRepository.save(category);
                log.info("Initialized default category: {}", name);
            }
        }
    }
}
