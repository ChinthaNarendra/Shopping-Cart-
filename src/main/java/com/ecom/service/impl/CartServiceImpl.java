package com.ecom.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.ecom.model.Cart;
import com.ecom.model.Product;
import com.ecom.model.UserDtls;
import com.ecom.repository.CartRepository;
import com.ecom.repository.ProductRepository;
import com.ecom.repository.UserRepository;
import com.ecom.service.CartService;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public Cart saveCart(Integer productId, Integer userId) {
        // retrieve user and product safely
        UserDtls userDtls = userRepository.findById(userId).orElse(null);
        Product product = productRepository.findById(productId).orElse(null);

        if (userDtls == null || product == null) {
            // either user or product not found -> cannot add to cart
            System.out.println("saveCart: user or product not found. userId=" + userId + " productId=" + productId);
            return null;
        }

        // check existing cart item for this user+product
        Cart cartStatus = cartRepository.findByProductIdAndUserId(productId, userId);
        Cart cart;

        if (ObjectUtils.isEmpty(cartStatus)) {
            cart = new Cart();
            cart.setProduct(product);
            cart.setUser(userDtls);
            cart.setQuantity(1);

            // use discount price if present else fallback to product price
            Double discountPrice = (product.getDiscountPrice() != null) ? product.getDiscountPrice() : null;
            double unitPrice = (discountPrice != null) ? discountPrice : (product.getPrice() != null ? product.getPrice() : 0.0);
            cart.setTotalPrice(unitPrice * cart.getQuantity());
        } else {
            cart = cartStatus;
            cart.setQuantity(cart.getQuantity() + 1);

            Double discountPrice = (cart.getProduct().getDiscountPrice() != null) ? cart.getProduct().getDiscountPrice() : null;
            double unitPrice = (discountPrice != null) ? discountPrice : (cart.getProduct().getPrice() != null ? cart.getProduct().getPrice() : 0.0);
            cart.setTotalPrice(unitPrice * cart.getQuantity());
        }

        // save and return the saved entity (important!)
        Cart savedCart = cartRepository.save(cart);
        System.out.println("saveCart: savedCart id = " + (savedCart != null ? savedCart.getId() : "null"));
        return savedCart;
    }

    @Override
    public List<Cart> getCartsByUser(Integer userId) {
        // assumes CartRepository has method List<Cart> findByUserId(Integer userId);
       List<Cart> carts = cartRepository.findByUserId(userId);
       Double totalOrderPrice = 0.0;
       List<Cart> updateCarts = new ArrayList<>();
       for (Cart c : carts) {
    	   Double totalPrice = (c.getProduct().getDiscountPrice() * c.getQuantity()) ;
    	   c.setTotalPrice(totalPrice);
    	   
    	   totalOrderPrice = totalOrderPrice + totalPrice;
    	   c.setTotalOrderPrice(totalOrderPrice);
    	   updateCarts.add(c);
       }
      
       
       
       return updateCarts;
    }

	@Override
	public Integer getCountCart(Integer userId) {
		Integer countByUserId = cartRepository.countByUserId(userId);
		return countByUserId;
	}

	@Override
	public void updateQuantity(String sy, Integer cid) {
		Cart cart = cartRepository.findById(cid).get();
		int updateQuantity;
		if (sy.equalsIgnoreCase("de")) {
			updateQuantity = cart.getQuantity()-1;
			if (updateQuantity <= 0) {
				cartRepository.delete(cart);
			} else {
				cart.setQuantity(updateQuantity);
				cartRepository.save(cart);
			}
		} else {
				updateQuantity = cart.getQuantity()+1;
				cart.setQuantity(updateQuantity);
				cartRepository.save(cart);
			}
			
			
		
		
	}
}
