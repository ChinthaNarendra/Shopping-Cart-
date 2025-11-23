package com.ecom.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.OrderService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ProductService productService;

	@Autowired
	private UserService userService;

	@Autowired
	private CartService cartService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private CommonUtil commonUtil;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	private static final long MAX_FILE_BYTES = 5L * 1024L * 1024L;
	
	/**
	 * Base upload directory (configure in application.properties:
	 * app.upload.dir=uploads)
	 */
	@Value("${app.upload.dir:uploads}")
	private String uploadDir;

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

	@GetMapping("/")
	public String index() {
		return "admin/index";
	}

	// ---------- CATEGORY MANAGEMENT ----------
	@GetMapping("/category")
	public String category(Model m,
	                       @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
	                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {

	    Page<Category> page = categoryService.getAllCategoryPagination(pageNo, pageSize);
	    List<Category> categories = page.getContent();

	    // List for table
	    m.addAttribute("categories", categories);

	    // ⭐ creates backing bean for form
	    if (!m.containsAttribute("category")) {
	        m.addAttribute("category", new Category());
	    }

	    // pagination
	    m.addAttribute("pageNo", page.getNumber());
	    m.addAttribute("pageSize", pageSize);
	    m.addAttribute("totalElements", page.getTotalElements());
	    m.addAttribute("totalPages", page.getTotalPages());
	    m.addAttribute("isFirst", page.isFirst());
	    m.addAttribute("isLast", page.isLast());

	    return "admin/category";
	}


	@PostMapping("/saveCategory")
	public String saveCategory(@ModelAttribute Category category,
			@RequestParam(value = "file", required = false) MultipartFile file, RedirectAttributes redirectAttrs) {

		try {
			Path base = Paths.get(uploadDir).toAbsolutePath();
			Files.createDirectories(base);

			Path saveDir = base.resolve("category_img");
			Files.createDirectories(saveDir);

			String imageName = "default.jpg";
			if (file != null && !file.isEmpty()) {

				// validate size
				if (file.getSize() > MAX_FILE_BYTES) {
					redirectAttrs.addFlashAttribute("errorMsg", "Uploaded file too large. Max allowed: 5 MB.");
					redirectAttrs.addFlashAttribute("category", category);
					return "redirect:/admin/category";
				}

				// validate content type
				String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
				if (!contentType.startsWith("image/")) {
					redirectAttrs.addFlashAttribute("errorMsg", "Invalid file type. Please upload an image.");
					redirectAttrs.addFlashAttribute("category", category);
					return "redirect:/admin/category";
				}

				String original = StringUtils.cleanPath(file.getOriginalFilename());
				String ext = "";
				if (original != null && original.contains(".")) {
					ext = original.substring(original.lastIndexOf('.'));
				}
				imageName = UUID.randomUUID().toString() + ext;
				Path targetPath = saveDir.resolve(imageName);
				Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
			}

			category.setImageName(imageName);
			if (category.getActive() == null) {
				category.setActive(Boolean.TRUE);
			}

			categoryService.saveCategory(category);
			redirectAttrs.addFlashAttribute("succMsg", "Category saved successfully!");
		} catch (IOException e) {
			e.printStackTrace();
			redirectAttrs.addFlashAttribute("errorMsg", "Error saving category: " + e.getMessage());
		}

		return "redirect:/admin/category";
	}

	/**
	 * Use POST for deletion (safer than GET). Template should submit a POST form to
	 * this endpoint.
	 */
	@PostMapping("/deleteCategory/{id}")
	public String deleteCategoryPost(@PathVariable Integer id, HttpSession session) {
	    if (categoryService.deleteCategory(id)) {
	        session.setAttribute("succMsg", "Category deleted successfully");
	    } else {
	        session.setAttribute("errorMsg", "Something went wrong");
	    }
	    return "redirect:/admin/category";
	}


	@GetMapping("/editCategory/{id}")
	public String loadEditCategory(@PathVariable("id") Integer id, Model m, RedirectAttributes ra) {
		Category existing = categoryService.getCategoryById(id);
		if (existing == null) {
			ra.addFlashAttribute("errorMsg", "Category not found");
			return "redirect:/admin/category";
		}

		m.addAttribute("category", existing);
		m.addAttribute("categories", categoryService.getAllCategories());
		return "admin/edit_category";
	}

	@PostMapping("/updateCategory")
	public String updateCategory(@ModelAttribute Category category,
			@RequestParam(value = "file", required = false) MultipartFile file, RedirectAttributes ra) {
		try {
			Category existing = categoryService.getCategoryById(category.getId());
			if (existing == null) {
				ra.addFlashAttribute("errorMsg", "Category not found.");
				return "redirect:/admin/category";
			}

			existing.setName(category.getName());
			existing.setActive(category.getActive() == null ? Boolean.TRUE : category.getActive());

			if (file != null && !file.isEmpty()) {

				// validate size
				if (file.getSize() > MAX_FILE_BYTES) {
					ra.addFlashAttribute("errorMsg", "Uploaded file too large. Max allowed: 5 MB.");
					ra.addFlashAttribute("category", category);
					return "redirect:/admin/category";
				}

				// validate content type
				String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
				if (!contentType.startsWith("image/")) {
					ra.addFlashAttribute("errorMsg", "Invalid file type. Please upload an image.");
					ra.addFlashAttribute("category", category);
					return "redirect:/admin/category";
				}

				Path base = Paths.get(uploadDir).toAbsolutePath();

				Path saveDir = base.resolve("category_img");
				Files.createDirectories(saveDir);

				String original = StringUtils.cleanPath(file.getOriginalFilename());
				String ext = "";
				if (original != null && original.contains(".")) {
					ext = original.substring(original.lastIndexOf('.'));
				}
				String newImageName = UUID.randomUUID().toString() + ext;
				Path target = saveDir.resolve(newImageName);
				Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

				if (existing.getImageName() != null && !existing.getImageName().equals("default.jpg")) {
					Files.deleteIfExists(saveDir.resolve(existing.getImageName()));
				}

				existing.setImageName(newImageName);
			}

			categoryService.saveCategory(existing);
			ra.addFlashAttribute("succMsg", "Category updated successfully.");
		} catch (Exception e) {
			e.printStackTrace();
			ra.addFlashAttribute("errorMsg", "Error updating category: " + e.getMessage());
		}
		return "redirect:/admin/category";
	}

	// ---------- PRODUCT MANAGEMENT ----------
	@GetMapping("/loadAddProduct")
	public String loadAddProduct(Model m) {
		m.addAttribute("categories", categoryService.getAllCategories());
		Product p = new Product();
		if (p.getIsActive() == null)
			p.setIsActive(Boolean.TRUE);
		m.addAttribute("product", p);
		return "admin/add_product";
	}

	@PostMapping("/saveProduct")
	public String saveProduct(@ModelAttribute Product product,
			@RequestParam(value = "file", required = false) MultipartFile file, RedirectAttributes ra) {
		String imageName = "default.jpg";

		try {
			Path base = Paths.get(uploadDir).toAbsolutePath();

			Files.createDirectories(base);
			Path saveDir = base.resolve("product_img");
			Files.createDirectories(saveDir);

			if (file != null && !file.isEmpty()) {

				// validate size
				if (file.getSize() > MAX_FILE_BYTES) {
					ra.addFlashAttribute("errorMsg", "Uploaded file too large. Max allowed: 5 MB.");
					ra.addFlashAttribute("product", product);
					return "redirect:/admin/loadAddProduct";
				}

				// validate content type
				String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
				if (!contentType.startsWith("image/")) {
					ra.addFlashAttribute("errorMsg", "Invalid file type. Please upload an image.");
					ra.addFlashAttribute("product", product);
					return "redirect:/admin/loadAddProduct";
				}

				String original = StringUtils.cleanPath(file.getOriginalFilename());
				String ext = "";
				if (original != null && original.contains(".")) {
					ext = original.substring(original.lastIndexOf('.'));
				}

				imageName = UUID.randomUUID().toString() + ext;
				Path targetPath = saveDir.resolve(imageName);
				Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
			}

			product.setImage(imageName);
			if (product.getIsActive() == null)
				product.setIsActive(Boolean.TRUE);

			computeAndSetDiscountPrice(product);

			productService.saveProduct(product);
			ra.addFlashAttribute("succMsg", "Product saved successfully!");
		} catch (Exception e) {
			e.printStackTrace();
			ra.addFlashAttribute("errorMsg", "Error saving product: " + e.getMessage());
		}

		return "redirect:/admin/loadAddProduct";
	}

	@GetMapping("/products")
	public String loadViewProduct(Model m, @RequestParam(value = "ch", required = false) String ch,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "5") Integer pageSize) {

		Page<Product> page = null;
		if (ch != null && ch.trim().length() > 0) {
			page = productService.searchProductPagination(pageNo, pageSize, ch);
		} else {
			page = productService.getAllProductsPagination(pageNo, pageSize);
		}

		m.addAttribute("products", page.getContent());
		// pagination attributes used by template
		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());
		return "admin/products";
	}

	/**
	 * Use POST for deletion from the UI. Template should post to this endpoint.
	 */
	@PostMapping("/deleteProduct/{id}")
	public String deleteProductPost(@PathVariable int id, HttpSession session) {
		if (productService.deleteProduct(id)) {
			session.setAttribute("succMsg", "Product deleted successfully");
		} else {
			session.setAttribute("errorMsg", "Something went wrong on server");
		}
		return "redirect:/admin/products";
	}

	@GetMapping("/editProduct/{id}")
	public String editProduct(@PathVariable int id, Model m, RedirectAttributes ra) {
		Product product = productService.getproductById(id);
		if (product == null) {
			ra.addFlashAttribute("errorMsg", "Product not found");
			return "redirect:/admin/products";
		}
		if (product.getIsActive() == null)
			product.setIsActive(Boolean.TRUE);
		if (product.getDiscountPrice() == null)
			product.setDiscountPrice(product.getPrice() == null ? 0.0 : product.getPrice());
		m.addAttribute("product", product);
		m.addAttribute("categories", categoryService.getAllCategories());
		return "admin/edit_product";
	}

	@PostMapping("/updateProduct")
	public String updateProduct(@ModelAttribute Product product,
			@RequestParam(value = "file", required = false) MultipartFile file, RedirectAttributes ra) {
		try {
			Product existing = productService.getproductById(product.getId());
			if (existing == null) {
				ra.addFlashAttribute("errorMsg", "Product not found.");
				return "redirect:/admin/products";
			}

			existing.setTitle(product.getTitle());
			existing.setDescription(product.getDescription());
			existing.setCategory(product.getCategory());
			existing.setPrice(product.getPrice());
			existing.setStock(product.getStock());
			existing.setDiscount(product.getDiscount());
			existing.setIsActive(product.getIsActive() == null ? Boolean.TRUE : product.getIsActive());

			if (file != null && !file.isEmpty()) {

				// validate size
				if (file.getSize() > MAX_FILE_BYTES) {
					ra.addFlashAttribute("errorMsg", "Uploaded file too large. Max allowed: 5 MB.");
					ra.addFlashAttribute("product", product);
					return "redirect:/admin/products";
				}

				// validate content type
				String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
				if (!contentType.startsWith("image/")) {
					ra.addFlashAttribute("errorMsg", "Invalid file type. Please upload an image.");
					ra.addFlashAttribute("product", product);
					return "redirect:/admin/products";
				}

				Path base = Paths.get(uploadDir).toAbsolutePath();

				Path saveDir = base.resolve("product_img");
				Files.createDirectories(saveDir);

				String original = StringUtils.cleanPath(file.getOriginalFilename());
				String ext = "";
				if (original != null && original.contains(".")) {
					ext = original.substring(original.lastIndexOf('.'));
				}
				String newImageName = UUID.randomUUID().toString() + ext;

				Path target = saveDir.resolve(newImageName);
				Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

				if (existing.getImage() != null && !existing.getImage().equals("default.jpg")) {
					Files.deleteIfExists(saveDir.resolve(existing.getImage()));
				}

				existing.setImage(newImageName);
			}

			computeAndSetDiscountPrice(existing);

			productService.saveProduct(existing);
			ra.addFlashAttribute("succMsg", "Product updated successfully!");
		} catch (Exception e) {
			e.printStackTrace();
			ra.addFlashAttribute("errorMsg", "Error updating product: " + e.getMessage());
		}

		return "redirect:/admin/products";
	}

	// ---------- Helper ----------
	private void computeAndSetDiscountPrice(Product product) {
		BigDecimal price = BigDecimal.ZERO;
		if (product.getPrice() != null) {
			price = BigDecimal.valueOf(product.getPrice());
		}

		Integer discountPercentObj = product.getDiscount();
		int discountPercent = (discountPercentObj == null) ? 0 : discountPercentObj;
		if (discountPercent < 0)
			discountPercent = 0;
		if (discountPercent > 100)
			discountPercent = 100;

		BigDecimal discountAmount = price.multiply(BigDecimal.valueOf(discountPercent)).divide(BigDecimal.valueOf(100));
		BigDecimal discounted = price.subtract(discountAmount);

		product.setDiscountPrice(discounted.doubleValue());
	}

	// ---------- USERS ----------
	@GetMapping("/users")
	public String getAllUsers(Model m,
	                          @RequestParam(name = "type", required = false, defaultValue = "1") Integer type) {
	    List<UserDtls> users;
	    if (type != null && type == 1) {
	        users = userService.getUsers("ROLE_USER");
	    } else {
	        users = userService.getUsers("ROLE_ADMIN");
	    }
	    m.addAttribute("userType", type);
	    m.addAttribute("users", users);
	    return "/admin/users";
	}

	@GetMapping("/updateSts")
	public String updateUserAccoutStatus(@RequestParam Boolean status,
	                                     @RequestParam Integer id,
	                                     @RequestParam Integer type,
	                                     HttpSession session) {
	    Boolean f = userService.updateAccountStatus(id, status);
	    if (f) {
	        session.setAttribute("succMsg", "Account Status Updated");
	    } else {
	        session.setAttribute("errorMsg", "Something wrong on server");
	    }
	    return "redirect:/admin/users?type=" + type;
	}


	// ---------- ORDERS ----------
	@GetMapping("/orders")
	public String getAllOrders(
	        @RequestParam(value = "ch", required = false) String ch,
	        Model m,
	        HttpSession session,
	        @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
	        @RequestParam(name = "pageSize", defaultValue = "2") Integer pageSize) {

	    // If a search string was provided -> try to return matching single order (or list)
	    if (ch != null && !ch.trim().isEmpty()) {
	        String query = ch.trim();
	        ProductOrder order = orderService.getOrdersByOrderId(query);

	        if (order == null) {
	            m.addAttribute("errorMsg", "Incorrect Order Id");
	            m.addAttribute("orders", List.of()); // empty list
	        } else {
	            m.addAttribute("orders", List.of(order)); // single result
	            m.addAttribute("succMsg", "1 order found.");
	        }

	        // still provide pagination attributes (so template doesn't NPE)
	        m.addAttribute("pageNo", 0);
	        m.addAttribute("pageSize", 1);
	        m.addAttribute("totalElements",  (order == null) ? 0L : 1L);
	        m.addAttribute("totalPages", 1);
	        m.addAttribute("isFirst", true);
	        m.addAttribute("isLast", true);

	        // render the admin/orders template
	        return "admin/orders";
	    }

	    // No search -> show paged list
	    Page<ProductOrder> page = orderService.getAllOrdersPagination(pageNo, pageSize);
	    m.addAttribute("orders", page.getContent());

	    // pagination attributes used by template
	    m.addAttribute("pageNo", page.getNumber());
	    m.addAttribute("pageSize", page.getSize());
	    m.addAttribute("totalElements", page.getTotalElements());
	    m.addAttribute("totalPages", page.getTotalPages());
	    m.addAttribute("isFirst", page.isFirst());
	    m.addAttribute("isLast", page.isLast());

	    return "admin/orders";
	}

	/**
	 * Admin updates order status (POST). Uses OrderStatus.fromId helper for lookup.
	 */
	@PostMapping("/update-order-status")
	public String updateOrderStatus(@RequestParam Integer id, @RequestParam Integer st, Principal p,
			RedirectAttributes ra) {

		if (id == null || st == null) {
			ra.addFlashAttribute("errorMsg", "Invalid request.");
			return "redirect:/admin/orders";
		}

		ProductOrder order = orderService.getOrderById(id);
		if (order == null) {
			ra.addFlashAttribute("errorMsg", "Order not found.");
			return "redirect:/admin/orders";
		}

		OrderStatus statusEnum = OrderStatus.fromId(st);
		if (statusEnum == null) {
			ra.addFlashAttribute("errorMsg", "Invalid status selected.");
			return "redirect:/admin/orders";
		}

		// Prevent cancelling a delivered order
		if (OrderStatus.DELIVERED.getName().equalsIgnoreCase(order.getStatus())
				&& OrderStatus.CANCELLED.getName().equalsIgnoreCase(statusEnum.getName())) {
			ra.addFlashAttribute("errorMsg", "Cannot cancel a delivered order.");
			return "redirect:/admin/orders";
		}

		// pass the textual status name to the service
		ProductOrder updated = orderService.updateOrderStatus(id, statusEnum.getName());
		try {
			if (updated != null) {
				commonUtil.sendMailForProductOrder(updated, statusEnum.getName());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (updated != null) {
			ra.addFlashAttribute("succMsg", "Status updated.");
		} else {
			ra.addFlashAttribute("errorMsg", "Status not updated.");
		}
		return "redirect:/admin/orders";
	}
	
	@GetMapping("/add-admin")
	public String loadAdminAdd(Model m) {
	    m.addAttribute("user", new UserDtls());  // Always blank
	    return "admin/add_admin";
	}

	
	 // --- process add-admin form ---
	    @PostMapping("/save-admin")
	    public String saveAdmin(@ModelAttribute UserDtls user,
	                            @RequestParam(name = "cpassword", required = false) String cpassword,
	                            @RequestParam(name = "img", required = false) MultipartFile file,
	                            RedirectAttributes redirectAttrs) {

	        try {
	            if (user == null) {
	                redirectAttrs.addFlashAttribute("errorMsg", "Form data missing.");
	                return "redirect:/admin/add-admin";
	            }

	            // helper to test empty strings
	            java.util.function.Predicate<String> empty = s -> (s == null || s.trim().isEmpty());

	            if (empty.test(user.getName()) ||
	                empty.test(user.getMobileNumber()) ||
	                empty.test(user.getEmail()) ||
	                empty.test(user.getAddress()) ||
	                empty.test(user.getCity()) ||
	                empty.test(user.getState()) ||
	                empty.test(user.getPinCode() == null ? "" : String.valueOf(user.getPinCode())) ||
	                empty.test(user.getPassword())) {

	                redirectAttrs.addFlashAttribute("errorMsg", "Please fill all required fields.");
	                redirectAttrs.addFlashAttribute("user", user);
	                return "redirect:/admin/add-admin";
	            }

	            // password match
	            String pwd = user.getPassword() == null ? "" : user.getPassword();
	            String cpw = cpassword == null ? "" : cpassword;
	            if (!pwd.equals(cpw)) {
	                redirectAttrs.addFlashAttribute("errorMsg", "Password and Confirm Password do not match.");
	                redirectAttrs.addFlashAttribute("user", user);
	                return "redirect:/admin/add-admin";
	            }

	            // file presence & basic validation (UI required file; but if you want optional make this conditional)
	            if (file == null || file.isEmpty()) {
	                redirectAttrs.addFlashAttribute("errorMsg", "Please upload a profile image.");
	                redirectAttrs.addFlashAttribute("user", user);
	                return "redirect:/admin/add-admin";
	            }

	            if (file.getSize() > MAX_FILE_BYTES) {
	                redirectAttrs.addFlashAttribute("errorMsg", "Profile image too large. Max allowed is 5 MB.");
	                redirectAttrs.addFlashAttribute("user", user);
	                return "redirect:/admin/add-admin";
	            }

	            String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
	            if (!contentType.contains("image")) {
	                redirectAttrs.addFlashAttribute("errorMsg", "Invalid file type. Please upload an image (jpg/png/webp/gif).");
	                redirectAttrs.addFlashAttribute("user", user);
	                return "redirect:/admin/add-admin";
	            }

	            // sanitize filename
	            String original = StringUtils.cleanPath(file.getOriginalFilename());
	            if (original.contains("..")) {
	                redirectAttrs.addFlashAttribute("errorMsg", "Invalid file name.");
	                redirectAttrs.addFlashAttribute("user", user);
	                return "redirect:/admin/add-admin";
	            }

	            // store file in uploads/profile_img/<uuid>.<ext>
	            String ext = "";
	            int dot = original.lastIndexOf('.');
	            if (dot >= 0) ext = original.substring(dot);

	            String filename = UUID.randomUUID().toString() + ext;

	            Path base = Paths.get(uploadDir).toAbsolutePath();                // <project-root>/uploads
	            Path profileDir = base.resolve("profile_img");                     // <project-root>/uploads/profile_img
	            Files.createDirectories(profileDir);

	            Path targetPath = profileDir.resolve(filename).normalize();
	            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

	            // store relative path in DB as "profile_img/<filename>" so Thymeleaf can use /uploads/${user.profileImage}
	            user.setProfileImage("profile_img/" + filename);

	            // save admin via service (service will encode password and set role)
	            UserDtls saved = userService.saveAdmin(user);
	            if (ObjectUtils.isEmpty(saved)) {
	                redirectAttrs.addFlashAttribute("errorMsg", "Something went wrong while saving user.");
	                redirectAttrs.addFlashAttribute("user", user);
	                // delete file we just wrote to avoid orphans (best effort)
	                try { Files.deleteIfExists(targetPath); } catch (Exception ex) { ex.printStackTrace(); }
	                return "redirect:/admin/add-admin";
	            }

	            redirectAttrs.addFlashAttribute("succMsg", "Admin registered successfully.");
	            return "redirect:/admin/add-admin";

	        } catch (IOException ex) {
	            ex.printStackTrace();
	            redirectAttrs.addFlashAttribute("errorMsg", "File upload error: " + ex.getMessage());
	            redirectAttrs.addFlashAttribute("user", user);
	            return "redirect:/admin/add-admin";
	        } catch (Exception ex) {
	            ex.printStackTrace();
	            redirectAttrs.addFlashAttribute("errorMsg", "Server error: " + ex.getMessage());
	            redirectAttrs.addFlashAttribute("user", user);
	            return "redirect:/admin/add-admin";
	        }
	    }
	    
	    @GetMapping("/profile")
	    public String profile() {
	    	return "/admin/profile";
	    }
	    
	    
	    
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
				return "redirect:/admin/profile";
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

			return "redirect:/admin/profile";
		}

		@PostMapping("/change-password")
		public String changePassword(@RequestParam String currentPassword,
		                             @RequestParam String newPassword,
		                             @RequestParam String confirmPassword,
		                             Principal p,
		                             RedirectAttributes redirectAttrs) {

		    UserDtls loggedInUser = commonUtil.getLoggedInUserDetails(p);

		    if (loggedInUser == null) {
		        redirectAttrs.addFlashAttribute("pwdError", "Please login first.");
		        return "redirect:/login";
		    }

		    // 1️⃣ Check new password = confirm password
		    if (!newPassword.equals(confirmPassword)) {
		        redirectAttrs.addFlashAttribute("pwdError", "New Password and Confirm Password do not match!");
		        return "redirect:/admin/profile";
		    }

		    // 2️⃣ Validate old password
		    boolean matches = passwordEncoder.matches(currentPassword, loggedInUser.getPassword());
		    if (!matches) {
		        redirectAttrs.addFlashAttribute("pwdError", "Current password is incorrect!");
		        return "redirect:/admin/profile";
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

		    return "redirect:/admin/profile";
		}

}
