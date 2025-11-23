package com.ecom.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ecom.model.OrderRequest;
import com.ecom.model.ProductOrder;

public interface OrderService {

    /**
     * Save an order for a user.
     */
    void saveOrder(Integer userId, OrderRequest orderRequest) throws Exception;

    /**
     * Get all orders for a given user.
     */
    List<ProductOrder> getOrderByUser(Integer userId);

    /**
     * Get a single order by DB id.
     * Returns null if not found (you can change to Optional<ProductOrder> if preferred).
     */
    ProductOrder getOrderById(Integer id);

    /**
     * Update the order's status (status is the name, e.g., "Cancelled", "Delivered").
     * Returns true if update succeeded.
     */
    ProductOrder updateOrderStatus(Integer id, String status);
    
    public List<ProductOrder> getAllOrders();
    
    
    public ProductOrder getOrdersByOrderId(String OrderId);
    
    
    public Page<ProductOrder> getAllOrdersPagination(Integer pageNo, Integer pageSize);
}
