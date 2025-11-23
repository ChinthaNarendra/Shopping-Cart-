package com.ecom.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ecom.model.Category;

public interface CategoryService {
    
    Category saveCategory(Category category);
    
    Boolean existCategory(String name);
    
    // ✅ Naming consistency fix: change to plural (matches Thymeleaf usage)
    List<Category> getAllCategories();
    
    public Boolean deleteCategory(int id);

	public Category getCategoryById(int id);
	
	public List<Category> getAllActiveCategory();
	
	public Page<Category> getAllCategoryPagination(Integer pageNo, Integer pageSize);
}
