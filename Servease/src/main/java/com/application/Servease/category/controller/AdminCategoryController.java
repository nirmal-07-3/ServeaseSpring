package com.application.Servease.category.controller;

import com.application.Servease.category.dto.CategoryResponse;
import com.application.Servease.category.dto.CreateCategoryRequest;
import com.application.Servease.category.dto.UpdateCategoryRequest;
import com.application.Servease.category.service.CategoryService;
import com.application.Servease.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "Admin - Category", description = "Category Management APIs")
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>>
    createCategory(
            @Valid
            @RequestBody
            CreateCategoryRequest request) {

        CategoryResponse response =
                categoryService.createCategory(request);

        return ResponseEntity.ok(

                new ApiResponse<>(

                        true,

                        "Category created successfully.",

                        response
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>>
    updateCategory(

            @PathVariable Long id,

            @Valid
            @RequestBody
            UpdateCategoryRequest request) {

        CategoryResponse response =
                categoryService.updateCategory(id, request);

        return ResponseEntity.ok(

                new ApiResponse<>(

                        true,

                        "Category updated successfully.",

                        response
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>>
    deleteCategory(
            @PathVariable Long id) {

        categoryService.deleteCategory(id);

        return ResponseEntity.ok(

                new ApiResponse<>(

                        true,

                        "Category deleted successfully.",

                        null
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>>
    getAllCategories() {

        List<CategoryResponse> response =
                categoryService.getAllCategories();

        return ResponseEntity.ok(

                new ApiResponse<>(

                        true,

                        "Categories fetched successfully.",

                        response
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>>
    getCategoryById(
            @PathVariable Long id) {

        CategoryResponse response =
                categoryService.getCategoryById(id);

        return ResponseEntity.ok(

                new ApiResponse<>(

                        true,

                        "Category fetched successfully.",

                        response
                )
        );
    }
}