package com.application.Servease.subcategory.repository;

import com.application.Servease.category.entity.Category;
import com.application.Servease.subcategory.entity.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {

    /**
     * Check duplicate subcategory name within the same category.
     */
    boolean existsByCategoryAndNameIgnoreCase(
            Category category,
            String name
    );

    /**
     * Find subcategory by category and name.
     */
    Optional<SubCategory> findByCategoryAndNameIgnoreCase(
            Category category,
            String name
    );

    /**
     * Admin - Fetch all subcategories.
     */
    List<SubCategory> findAllByOrderByDisplayOrderAscNameAsc();

    /**
     * Customer - Fetch only active subcategories of a category.
     */
    List<SubCategory> findByCategoryAndActiveTrueOrderByDisplayOrderAscNameAsc(
            Category category
    );

    /**
     * Admin - Fetch all subcategories of a category.
     */
    List<SubCategory> findByCategoryOrderByDisplayOrderAscNameAsc(
            Category category
    );

    /**
     * Customer - Fetch only active subcategory by id.
     */
    Optional<SubCategory> findByIdAndActiveTrue(Long id);
}