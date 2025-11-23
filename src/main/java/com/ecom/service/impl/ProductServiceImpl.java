package com.ecom.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.ecom.model.Product;
import com.ecom.repository.ProductRepository;
import com.ecom.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public Product saveProduct(Product product) {
        // compute discount price before saving
        computeDiscountPrice(product);
        return productRepository.save(product);
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Boolean deleteProduct(Integer id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isPresent() && !ObjectUtils.isEmpty(productOpt.get())) {
            productRepository.delete(productOpt.get());
            return true;
        }
        return false;
    }

    @Override
    public Product getproductById(Integer id) {
        return productRepository.findById(id).orElse(null);
    }

    @Override
    public Product computeDiscountPrice(Product product) {
        if (product == null) return null;

        BigDecimal price = BigDecimal.ZERO;
        if (product.getPrice() != null) {
            price = BigDecimal.valueOf(product.getPrice());
        }

        Integer discObj = product.getDiscount();
        int disc = (discObj == null) ? 0 : discObj;
        if (disc < 0) disc = 0;
        if (disc > 100) disc = 100;

        BigDecimal discountPercent = BigDecimal.valueOf(disc);
        BigDecimal discountAmount = price.multiply(discountPercent).divide(BigDecimal.valueOf(100));
        BigDecimal discounted = price.subtract(discountAmount);

        product.setDiscountPrice(discounted.doubleValue());
        return product;
    }

    @Override
    public List<Product> getAllActiveProducts(String category) {
        if (ObjectUtils.isEmpty(category)) {
            return productRepository.findByIsActiveTrue();
        } else {
            // return only active products in category (case-insensitive)
            return productRepository.findByIsActiveTrueAndCategoryIgnoreCase(category);
        }
    }

    @Override
    public List<Product> searchProduct(String ch) {
        return productRepository.findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(ch, ch);
    }

    @Override
    public Page<Product> getAllActiveProductPagination(Integer pageNo, Integer pageSize, String category) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        if (ObjectUtils.isEmpty(category)) {
            return productRepository.findByIsActiveTrue(pageable);
        } else {
            // ensure active + category filtering
            return productRepository.findByIsActiveTrueAndCategoryIgnoreCase(category, pageable);
        }
    }

    @Override
    public Page<Product> searchProductPagination(Integer pageNo, Integer pageSize, String ch) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return productRepository.findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(ch, ch, pageable);
    }

    @Override
    public Page<Product> getAllProductsPagination(Integer pageNo, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return productRepository.findAll(pageable);
    }

    @Override
    public Page<Product> searchActiveProductPagination(Integer pageNo, Integer pageSize, String category, String ch) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);

        // if category is provided, restrict search to that category and active = true
        if (!ObjectUtils.isEmpty(category)) {
            // search within active products of the category (case-insensitive)
            // we'll perform a title/category contains check but restrict to the category
            // simple approach: if you want to search *within* a category by title, use repository methods or a custom @Query.
            // Here, if ch blank -> return active category page; else filter by title OR category but still restrict to category:
            if (ObjectUtils.isEmpty(ch)) {
                return productRepository.findByIsActiveTrueAndCategoryIgnoreCase(category, pageable);
            } else {
                // search across active products but restrict category equality (category matches)
                // Use repository query for category + title contains - create a custom @Query if needed.
                // Simple workaround: use repository.findByCategoryIgnoreCase(category, pageable) and then filter in memory is heavy.
                // Recommended: add a repository method (JPQL) for active + category + (title LIKE ... OR category LIKE ...).
                // For now let's use the searchActiveProducts and then filter by category in memory (ok for small datasets).
                Page<Product> candidates = productRepository.searchActiveProducts(ch, pageable);
                // if category provided, filter page content to only those with given category (case-insensitive)
                List<Product> filtered = candidates.getContent().stream()
                        .filter(p -> p.getCategory() != null && p.getCategory().equalsIgnoreCase(category))
                        .toList();
                return new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
            }
        } else {
            // category blank -> search across active products
            return productRepository.searchActiveProducts(ch, pageable);
        }
    }
}
