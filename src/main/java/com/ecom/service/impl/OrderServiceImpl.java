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
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	private ProductOrderRepository orderRepository;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private CommonUtil commonUtil;

	/**
	 * Save an order for every cart item of the user. This method is transactional
	 * so either all orders are saved or none.
	 */
	@Override
	@Transactional
	public void saveOrder(Integer userId, OrderRequest orderRequest) {
		// fetch user's carts
		List<Cart> carts = cartRepository.findByUserId(userId);
		if (carts == null || carts.isEmpty()) {
			// nothing to order
			return;
		}

		// create an OrderAddress once and reuse (or create per order if you prefer)
		OrderAddress address = new OrderAddress();
		address.setFirstName(orderRequest.getFirstName());
		address.setLastName(orderRequest.getLastName());
		address.setEmail(orderRequest.getEmail());
		address.setMobileNo(orderRequest.getMobileNo());
		address.setAddress(orderRequest.getAddress());
		address.setCity(orderRequest.getCity());
		address.setState(orderRequest.getState());
		address.setPincode(orderRequest.getPincode());

		for (Cart cart : carts) {
			ProductOrder order = new ProductOrder();

			// generate a business order id (readable) and set DB id is handled by JPA
			order.setOrderId(UUID.randomUUID().toString());
			order.setOrderDate(new Date());
			order.setProduct(cart.getProduct());
			order.setQuantity(cart.getQuantity());
			order.setUser(cart.getUser());
			order.setStatus(OrderStatus.IN_PROGRESS.getName());
			order.setPaymentType(orderRequest.getPaymentType());

			// Determine unit price defensively:
			// prefer product discount price -> product price -> cart total/quantity -> 0
			Double unitPrice = null;
			if (cart.getProduct() != null) {
				try {
					// try discountPrice first (field name may vary in your Product entity)
					unitPrice = cart.getProduct().getDiscountPrice();
					if (unitPrice == null) {
						unitPrice = cart.getProduct().getPrice(); // fallback to base price
					}
				} catch (Exception e) {
					unitPrice = null;
				}
			}
			if ((unitPrice == null || unitPrice == 0.0) && cart.getTotalPrice() != null && cart.getQuantity() != null
					&& cart.getQuantity() > 0) {
				unitPrice = cart.getTotalPrice() / cart.getQuantity();
			}
			if (unitPrice == null) {
				unitPrice = 0.0;
			}
			order.setPrice(unitPrice);

			// attach the same address instance (or clone if you prefer separate DB rows)
			order.setOrderAddress(address);

			ProductOrder saveOrder = orderRepository.save(order);
			try {
				// commonUtil.sendMailForProductOrder(saveOrder, "success");
			} catch (Exception e) {

				e.printStackTrace();
			}
		}

		// OPTIONAL: clear user's cart after successful order placement
		// If you want to keep carts (e.g., for history), remove this line.
		// Use deleteAllInBatch for efficiency if repository supports it.
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

			ProductOrder updateOrder = orderRepository.save(productOrder);

			return updateOrder;
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
	public Page<ProductOrder> getAllOrdersPagination(Integer pageNo, Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return orderRepository.findAll(pageable);
	}
}
