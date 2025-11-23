package com.ecom.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.ecom.model.Category;
import com.ecom.repository.CategoryRepository;
import com.ecom.service.CategoryService;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    // corrected method name (plural)
    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // keep Boolean as in your service interface if you prefer wrapper; here we return Boolean for compatibility
    @Override
    public Boolean existCategory(String name) {
        // delegate to repository's case-insensitive method
        return categoryRepository.existsByNameIgnoreCase(name);
    }

    // optional helpers you may want (not required by the interface)
    public Optional<Category> getCategoryById(Integer id) {
        return categoryRepository.findById(id);
    }

    public void deleteCategory(Integer id) {
        categoryRepository.deleteById(id);
    }

	@Override
	public Boolean deleteCategory(int id) {
		Category category = categoryRepository.findById(id).orElse(null);
		
		if (!ObjectUtils.isEmpty(category)) {
			categoryRepository.delete(category);
			return true;
		}
		return false;
	}

	@Override
	public Category getCategoryById(int id) {
		Category category = categoryRepository.findById(id).orElse(null);
		return category;
	}

	@Override
	public List<Category> getAllActiveCategory() {
		List<Category> categories = categoryRepository.findAllByActiveTrue();
		return categories;
	}

	@Override
	public Page<Category> getAllCategoryPagination(Integer pageNo, Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return categoryRepository.findAll(pageable);
	}

	
}
