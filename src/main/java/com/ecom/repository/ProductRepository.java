package com.ecom.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecom.model.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findByIsActiveTrue();

    // category methods (case-insensitive)
    List<Product> findByCategoryIgnoreCase(String category);

    List<Product> findByIsActiveTrueAndCategoryIgnoreCase(String category);

    List<Product> findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(String ch, String ch2);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    // Pageable methods: Pageable must be the last parameter
    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);

    Page<Product> findByIsActiveTrueAndCategoryIgnoreCase(String category, Pageable pageable);

    Page<Product> findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(String ch, String ch2, Pageable pageable);

    /**
     * Explicit query to ensure the predicate groups correctly:
     * WHERE is_active = true AND (lower(title) LIKE %:ch% OR lower(category) LIKE %:ch%)
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :ch, '%')) OR LOWER(p.category) LIKE LOWER(CONCAT('%', :ch, '%')))")
    Page<Product> searchActiveProducts(@Param("ch") String ch, Pageable pageable);
}
