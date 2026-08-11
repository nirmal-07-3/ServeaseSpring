package com.application.Servease.subcategory.controller;

import com.application.Servease.common.dto.ApiResponse;
import com.application.Servease.subcategory.dto.CreateSubCategoryRequest;
import com.application.Servease.subcategory.dto.SubCategoryResponse;
import com.application.Servease.subcategory.dto.UpdateSubCategoryRequest;
import com.application.Servease.subcategory.dto.UpdateSubCategoryStatusRequest;
import com.application.Servease.subcategory.service.SubCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/subcategories")
@RequiredArgsConstructor
@Tag(name = "Admin - SubCategory", description = "SubCategory Management APIs")
public class SubCategoryAdminController {

    private final SubCategoryService subCategoryService;

    @Operation(summary = "Create SubCategory")
    @PostMapping
    public ResponseEntity<ApiResponse<SubCategoryResponse>> createSubCategory(
            @Valid @RequestBody CreateSubCategoryRequest request) {

        SubCategoryResponse response =
                subCategoryService.createSubCategory(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "SubCategory created successfully.",
                        response
                )
        );
    }

    @Operation(summary = "Update SubCategory")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubCategoryResponse>> updateSubCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubCategoryRequest request) {

        SubCategoryResponse response =
                subCategoryService.updateSubCategory(id, request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "SubCategory updated successfully.",
                        response
                )
        );
    }

    @Operation(summary = "Delete SubCategory")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSubCategory(
            @PathVariable Long id) {

        subCategoryService.deleteSubCategory(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "SubCategory deleted successfully.",
                        null
                )
        );
    }

    @Operation(summary = "Update SubCategory Status")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubCategoryStatusRequest request) {

        subCategoryService.updateSubCategoryStatus(id, request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "SubCategory status updated successfully.",
                        null
                )
        );
    }

    @Operation(summary = "Get All SubCategories")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SubCategoryResponse>>> getAllSubCategories() {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "SubCategories fetched successfully.",
                        subCategoryService.getAllSubCategories()
                )
        );
    }

    @Operation(summary = "Get SubCategory By Id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubCategoryResponse>> getSubCategoryById(
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