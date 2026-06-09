package com.ecom.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecom.model.Cart;
import com.ecom.model.Category;
import com.ecom.model.OrderRequest;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.OrderService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserService userService;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private CartService cartService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@GetMapping("/")
	public String home() {
		return "user/home";
	}

	/**
	 * Common model attributes for user pages. safe: only add user-specific attrs
	 * when Principal present.
	 */
	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {
		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			if (userDtls != null) {
				m.addAttribute("user", userDtls);
				long countCart = cartService.getCountCart(userDtls.getId());
				m.addAttribute("countCart", countCart);
			}
		}
		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);
	}

	/**
	 * Add to cart: accepts optional uid param (for compatibility) but prefers
	 * logged-in user.
	 */
	@GetMapping("/addCart")
	public String addToCart(@RequestParam Integer pid, @RequestParam(required = false) Integer uid, Principal p,
			RedirectAttributes redirectAttrs) {

		Integer userId = uid;
		if (userId == null) {
			if (p == null) {
				redirectAttrs.addFlashAttribute("errorMsg", "Please login to add product to cart.");
				return "redirect:/login";
			}
			UserDtls logged = getLoggedInUserDetails(p);
			if (logged == null) {
				redirectAttrs.addFlashAttribute("errorMsg", "User not found.");
				return "redirect:/login";
			}
			userId = logged.getId();
		}

		Cart saveCart = cartService.saveCart(pid, userId);

		if (saveCart == null) {
			redirectAttrs.addFlashAttribute("errorMsg", "Product add to cart failed");
		} else {
			redirectAttrs.addFlashAttribute("succMsg", "Product added to cart");
		}

		return "redirect:/product/" + pid;
	}

	/**
	 * Load cart page. Requires authentication; otherwise redirect to login.
	 */
	@GetMapping("/cart")
	public String loadCartPage(Principal p, Model m, RedirectAttributes ra) {

		UserDtls user = getLoggedInUserDetails(p);

		if (user == null) {
			ra.addFlashAttribute("errorMsg", "Please login to view your cart.");
			return "redirect:/login";
		}

		List<Cart> carts = cartService.getCartsByUser(user.getId());
		m.addAttribute("carts", carts);

		double totalOrderPrice = 0.0;

		if (carts != null && !carts.isEmpty()) {
			for (Cart c : carts) {
				Double tp = c.getTotalPrice();
				totalOrderPrice += (tp == null ? 0.0 : tp);
			}
		}

		m.addAttribute("totalOrderPrice", totalOrderPrice);

		return "user/cart"; // <-- FIX
	}

	/**
	 * Update quantity from UI. sy = "in" or "de", cid = cart id. (Consider
	 * validating ownership inside service.)
	 */
	@GetMapping("/cartQuantityUpdate")
	public String updateCartQuantity(@RequestParam String sy, @RequestParam Integer cid) {
		cartService.updateQuantity(sy, cid);
		return "redirect:/user/cart";
	}

	private UserDtls getLoggedInUserDetails(Principal p) {
		if (p == null) {
			return null;
		}
		String email = p.getName();
		UserDtls userDtls = userService.getUserByEmail(email);
		return userDtls;
	}

	@GetMapping("/orders")
	public String orderPage(Principal p, Model m, RedirectAttributes ra) {
		UserDtls user = getLoggedInUserDetails(p);
		if (user == null) {
			ra.addFlashAttribute("errorMsg", "Please login to view orders.");
			return "redirect:/login";
		}

		List<Cart> carts = cartService.getCartsByUser(user.getId());
		m.addAttribute("carts", carts);

		double orderPrice = 0.0;
		if (carts != null && !carts.isEmpty()) {
			for (Cart c : carts) {
				Double tp = c.getTotalPrice(); // ensure Cart.getTotalPrice() returns per-cart total
				orderPrice += (tp == null ? 0.0 : tp);
			}
		}
		double shipping = 100.0; // or read from config
		double taxOrOther = 50.0;
		double totalOrderPrice = orderPrice + shipping + taxOrOther;

		m.addAttribute("orderPrice", orderPrice);
		m.addAttribute("totalOrderPrice", totalOrderPrice);

		return "user/order";
	}

	/**
	 * Save order (POST). Redirect after save to avoid double submit.
	 * 
	 * @throws Exception
	 */
	@PostMapping("/save-order")
	public String saveOrder(@ModelAttribute OrderRequest request, Principal p, RedirectAttributes ra) throws Exception {

		UserDtls user = getLoggedInUserDetails(p);
		if (user == null) {
			ra.addFlashAttribute("errorMsg", "Please login to place an order.");
			return "redirect:/login";
		}

		orderService.saveOrder(user.getId(), request);
		// you can set a success message or order id in flash attributes
		ra.addFlashAttribute("succMsg", "Order placed successfully!");
		return "redirect:/user/success";
	}

	@GetMapping("/success")
	public String loadSuccess() {
		return "/user/success";
	}

	@GetMapping("/user-orders")
	public String myOrder(Model m, Principal p, RedirectAttributes ra) {
		UserDtls loginUser = getLoggedInUserDetails(p);
		if (loginUser == null) {
			ra.addFlashAttribute("errorMsg", "Please login to view your orders.");
			return "redirect:/login";
		}
		List<ProductOrder> orders = orderService.getOrderByUser(loginUser.getId());
		m.addAttribute("orders", orders);
		return "user/my_orders";
	}

	/**
	 * Update order status. Use POST since this changes state. Validates ownership:
	 * only order owner (or admins if you add that check) can update.
	 */
	@PostMapping("/update-status")
	public String updateOrderStatus(@RequestParam Integer id, @RequestParam Integer st, Principal p,
			RedirectAttributes ra) {

		if (id == null || st == null) {
			ra.addFlashAttribute("errorMsg", "Invalid request.");
			return "redirect:/user/user-orders";
		}

		UserDtls loginUser = getLoggedInUserDetails(p);
		if (loginUser == null) {
			ra.addFlashAttribute("errorMsg", "Please login to update orders.");
			return "redirect:/login";
		}

		ProductOrder order = orderService.getOrderById(id);
		if (order == null) {
			ra.addFlashAttribute("errorMsg", "Order not found.");
			return "redirect:/user/user-orders";
		}

		if (order.getUser() == null || !order.getUser().getId().equals(loginUser.getId())) {
			ra.addFlashAttribute("errorMsg", "You are not authorized to update this order.");
			return "redirect:/user/user-orders";
		}

		String status = null;
		for (OrderStatus orderSt : OrderStatus.values()) {
			if (orderSt.getId().equals(st)) {
				status = orderSt.getName();
				break;
			}
		}
		if (status == null) {
			ra.addFlashAttribute("errorMsg", "Invalid status selected.");
			return "redirect:/user/user-orders";
		}

		if ("Delivered".equalsIgnoreCase(order.getStatus()) && "Cancelled".equalsIgnoreCase(status)) {
			ra.addFlashAttribute("errorMsg", "Cannot cancel a delivered order.");
			return "redirect:/user/user-orders";
		}

		ProductOrder updated = orderService.updateOrderStatus(id, status);
		try {
			if (updated != null) {
				commonUtil.sendMailForProductOrder(updated, status);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (updated != null) {
			ra.addFlashAttribute("succMsg", "Status updated.");
		} else {
			ra.addFlashAttribute("errorMsg", "Status not updated.");
		}
		return "redirect:/user/user-orders";
	}

	@GetMapping("/profile")
	public String profile() {
		return "user/profile";
	}

	// inside com.ecom.controller.UserController
	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute("user") UserDtls user,
			@RequestParam(name = "imageFile", required = false) MultipartFile imageFile, Principal principal,
			RedirectAttributes redirectAttrs) {

		// require login
		if (principal == null) {
			redirectAttrs.addFlashAttribute("errorMsg", "You must be logged in to update profile.");
			return "redirect:/login";
		}

		String loggedEmail = principal.getName();
		UserDtls loggedUser = userService.getUserByEmail(loggedEmail);
		if (loggedUser == null) {
			redirectAttrs.addFlashAttribute("errorMsg", "User not found.");
			return "redirect:/login";
		}

		// ensure user.id belongs to logged in user (prevents tampering)
		if (user.getId() == null || !user.getId().equals(loggedUser.getId())) {
			redirectAttrs.addFlashAttribute("errorMsg", "Invalid update request.");
			return "redirect:/user/profile";
		}

		try {
			UserDtls updated = userService.updateUserProfile(user, imageFile);
			if (updated == null) {
				redirectAttrs.addFlashAttribute("errorMsg", "Profile update failed.");
			} else {
				redirectAttrs.addFlashAttribute("succMsg", "Profile updated successfully.");
			}
		} catch (Exception ex) {
			// log ex in real app
			redirectAttrs.addFlashAttribute("errorMsg", "Error updating profile: " + ex.getMessage());
		}

		return "redirect:/user/profile";
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam String currentPassword,
			@RequestParam String newPassword,
			@RequestParam String confirmPassword,
			Principal p,
			RedirectAttributes redirectAttrs) {

		UserDtls loggedInUser = getLoggedInUserDetails(p);

		if (loggedInUser == null) {
			redirectAttrs.addFlashAttribute("pwdError", "Please login first.");
			return "redirect:/login";
		}

		// 1️⃣ Check new password = confirm password
		if (!newPassword.equals(confirmPassword)) {
			redirectAttrs.addFlashAttribute("pwdError", "New Password and Confirm Password do not match!");
			return "redirect:/user/profile";
		}

		// 2️⃣ Validate old password
		boolean matches = passwordEncoder.matches(currentPassword, loggedInUser.getPassword());
		if (!matches) {
			redirectAttrs.addFlashAttribute("pwdError", "Current password is incorrect!");
			return "redirect:/user/profile";
		}

		// 3️⃣ Update password
		String encoded = passwordEncoder.encode(newPassword);
		loggedInUser.setPassword(encoded);

		UserDtls updated = userService.updateUser(loggedInUser);

		if (updated == null) {
			redirectAttrs.addFlashAttribute("pwdError", "Server error: Password not updated.");
		} else {
			redirectAttrs.addFlashAttribute("pwdSuccess", "Password updated successfully!");
		}

		return "redirect:user/profile";
	}

}
