package com.grim.backend.category.controller;

import com.grim.backend.auth.dto.ApiResponse;
import com.grim.backend.auth.dto.MessageResponse;
import com.grim.backend.auth.security.CustomUserDetails;
import com.grim.backend.category.dto.CategoryResponse;
import com.grim.backend.category.dto.CreateCategoryRequest;
import com.grim.backend.category.entity.Category;
import com.grim.backend.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<CategoryResponse> categories = categoryService.getCategories(userDetails.getId()).stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.isDefault()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, categories));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateCategoryRequest request) {
        Category category = categoryService.createCategory(userDetails.getId(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, new CategoryResponse(category.getId(), category.getName(), category.isDefault())));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody CreateCategoryRequest request) {
        Category category = categoryService.updateCategory(userDetails.getId(), id, request.name());
        return ResponseEntity.ok(new ApiResponse<>(true, new CategoryResponse(category.getId(), category.getName(), category.isDefault())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        categoryService.deleteCategory(userDetails.getId(), id);
        return ResponseEntity.ok(new ApiResponse<>(true, new MessageResponse("Category deleted. Transactions reassigned to Uncategorized.")));
    }
}
