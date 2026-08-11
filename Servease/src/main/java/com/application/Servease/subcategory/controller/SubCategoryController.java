package com.application.Servease.subcategory.controller;

import com.application.Servease.common.dto.ApiResponse;
import com.application.Servease.subcategory.dto.SubCategoryResponse;
import com.application.Servease.subcategory.service.SubCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "SubCategory", description = "Public SubCategory APIs")
public class SubCategoryController {

    private final SubCategoryService subCategoryService;

    @Operation(summary = "Get SubCategories By Category")
    @GetMapping("/api/categories/{categoryId}/subcategories")
    public ResponseEntity<ApiResponse<List<SubCategoryResponse>>> getByCategory(
            @PathVariable Long categoryId) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "SubCategories fetched successfully.",
                        subCategoryService.getSubCategoriesByCategory(categoryId)
                )
        );
    }

    @Operation(summary = "Get Active SubCategory By Id")
    @GetMapping("/api/subcategories/{id}")
    public ResponseEntity<ApiResponse<SubCategoryResponse>> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "SubCategory fetched successfully.",
                        subCategoryService.getSubCategoryById(id)
                )
        );
    }
}