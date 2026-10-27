package com.ecom.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.model.Cart;
import com.ecom.model.OrderAddress;
import com.ecom.model.OrderRequest;
import com.ecom.model.ProductOrder;
import com.ecom.repository.CartRepository;
import com.ecom.repository.ProductOrderRepository;
import com.ecom.service.OrderService;
import com.ecom.util.OrderStatus;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ProductOrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private com.ecom.util.CommonUtil commonUtil;

    /**
     * Saves an order for each cart item.
     * If any operation fails, the transaction is rolled back.
     */
    @Override
    @Transactional
    public void saveOrder(Integer userId, OrderRequest orderRequest) {

        // Fetch cart items belonging to the user
        List<Cart> carts = cartRepository.findByUserId(userId);

        if (carts == null || carts.isEmpty()) {
            return;
        }

        for (Cart cart : carts) {

            // Create a separate address record for each order
            OrderAddress address = new OrderAddress();

            address.setFirstName(orderRequest.getFirstName());
            address.setLastName(orderRequest.getLastName());
            address.setEmail(orderRequest.getEmail());
            address.setMobileNo(orderRequest.getMobileNo());
            address.setAddress(orderRequest.getAddress());
            address.setCity(orderRequest.getCity());
            address.setState(orderRequest.getState());
            address.setPincode(orderRequest.getPincode());

            ProductOrder order = new ProductOrder();

            // Generate a unique business order ID
            order.setOrderId(UUID.randomUUID().toString());

            order.setOrderDate(new Date());
            order.setProduct(cart.getProduct());
            order.setQuantity(cart.getQuantity());
            order.setUser(cart.getUser());
            order.setStatus(OrderStatus.IN_PROGRESS.getName());
            order.setPaymentType(orderRequest.getPaymentType());

            // Determine the unit price
            Double unitPrice = null;

            if (cart.getProduct() != null) {
                unitPrice = cart.getProduct().getDiscountPrice();

                if (unitPrice == null) {
                    unitPrice = cart.getProduct().getPrice();
                }
            }

            // Fall back to cart total divided by quantity
            if ((unitPrice == null || unitPrice == 0.0)
                    && cart.getTotalPrice() != null
                    && cart.getQuantity() != null
                    && cart.getQuantity() > 0) {

                unitPrice = cart.getTotalPrice() / cart.getQuantity();
            }

            if (unitPrice == null) {
                unitPrice = 0.0;
            }

            order.setPrice(unitPrice);

            // Attach the newly created address to this order
            order.setOrderAddress(address);

            // Save the order
            orderRepository.save(order);
        }

        // Clear the user's cart after all orders are saved
        cartRepository.deleteAll(carts);
    }

    @Override
    public List<ProductOrder> getOrderByUser(Integer userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public ProductOrder getOrderById(Integer id) {
        Optional<ProductOrder> opt = orderRepository.findById(id);
        return opt.orElse(null);
    }

    @Override
    public ProductOrder updateOrderStatus(Integer id, String status) {

        Optional<ProductOrder> findById = orderRepository.findById(id);

        if (findById.isPresent()) {

            ProductOrder productOrder = findById.get();

            productOrder.setStatus(status);

            return orderRepository.save(productOrder);
        }

        return null;
    }

    @Override
    public List<ProductOrder> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public ProductOrder getOrdersByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId);
    }

    @Override
    public Page<ProductOrder> getAllOrdersPagination(
            Integer pageNo, Integer pageSize) {

        Pageable pageable = PageRequest.of(pageNo, pageSize);

        return orderRepository.findAll(pageable);
    }
}
