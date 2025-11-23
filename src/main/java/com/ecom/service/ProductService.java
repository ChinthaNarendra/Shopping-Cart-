package com.ecom.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ecom.model.Product;

public interface ProductService {

    Product saveProduct(Product product);

    List<Product> getAllProducts();

    Boolean deleteProduct(Integer id);

    Product getproductById(Integer id);

    /**
     * Helper: compute and set discountPrice on product using price and discount (int percent)
     * Returns the same product (with discountPrice set).
     */
    Product computeDiscountPrice(Product product);

    /**
     * Return active products; if category is empty/blank return all active products,
     * otherwise return active products of that category (case-insensitive).
     */
    List<Product> getAllActiveProducts(String category);

    List<Product> searchProduct(String ch);

    /**
     * Return a Spring Data Page of active products for pagination.
     */
	Page<Product> getAllActiveProductPagination(Integer pageNo, Integer pageSize, String category);

	Page<Product> searchProductPagination(Integer pageNo, Integer pageSize, String ch);

	Page<Product> getAllProductsPagination(Integer pageNo, Integer pageSize);

	Page<Product> searchActiveProductPagination(Integer pageNo, Integer pageSize, String category, String ch);

}
