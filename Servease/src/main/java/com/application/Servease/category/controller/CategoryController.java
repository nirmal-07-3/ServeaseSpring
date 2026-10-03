package com.application.Servease.category.controller;

import com.application.Servease.category.dto.CategoryResponse;
import com.application.Servease.category.service.CategoryService;
import com.application.Servease.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Category",
        description = "Public Category APIs"
)
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(
            summary = "Get all active categories",
            description = "Fetch all active categories for customers and providers"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {

        List<CategoryResponse> categories =
                categoryService.getAllCategories();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Categories fetched successfully.",
                        categories
                )
        );
    }

    @Operation(
            summary = "Get active category by ID",
            description = "Fetch a category by its ID"
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(
            @PathVariable Long id) {

        CategoryResponse category =
                categoryService.getCategoryById(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Category fetched successfully.",
                        category
                )
        );
    }
}