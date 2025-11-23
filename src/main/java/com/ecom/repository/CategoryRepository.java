package com.ecom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecom.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // ✅ Checks if a category exists (case-insensitive)
    boolean existsByNameIgnoreCase(String name);

    // ✅ Fetch only active categories (works because field is named "active")
    List<Category> findAllByActiveTrue();
    
}
