package com.grim.backend.category.service;

import com.grim.backend.auth.entity.User;
import com.grim.backend.auth.repository.UserRepository;
import com.grim.backend.category.entity.Category;
import com.grim.backend.category.repository.CategoryRepository;
import com.grim.backend.common.exception.ConflictException;
import com.grim.backend.common.exception.ForbiddenActionException;
import com.grim.backend.common.exception.ResourceNotFoundException;
import com.grim.backend.transaction.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public List<Category> getCategories(UUID userId) {
        return categoryRepository.findAllForUser(userId);
    }

    @Transactional
    public Category createCategory(UUID userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (categoryRepository.findByNameAndUser(name, user).isPresent()) {
            throw new ConflictException("Category name already exists");
        }

        Category category = Category.builder()
                .user(user)
                .name(name)
                .isDefault(false)
                .build();

        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(UUID userId, UUID categoryId, String name) {
        Category category = categoryRepository.findByIdAndUser(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or not accessible"));

        if (category.isDefault()) {
            throw new ForbiddenActionException("Cannot modify system category");
        }

        category.setName(name);
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(UUID userId, UUID categoryId) {
        Category category = categoryRepository.findByIdAndUser(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or not accessible"));

        if (category.isDefault()) {
            throw new ForbiddenActionException("Cannot delete system category");
        }

        Category uncategorized = categoryRepository.findByNameAndIsDefaultTrue("Uncategorized")
                .orElseThrow(() -> new ResourceNotFoundException("Default Uncategorized category not found"));

        transactionRepository.reassignCategory(userId, categoryId, uncategorized.getId());
        categoryRepository.delete(category);
    }
}
