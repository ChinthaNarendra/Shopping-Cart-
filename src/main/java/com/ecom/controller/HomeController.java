package com.ecom.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

	// 5 MB limit
	private static final long MAX_FILE_BYTES = 5L * 1024L * 1024L;

	// upload directory injected from application.properties (default "uploads")
	@Value("${app.upload.dir:uploads}")
	private String uploadDir;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ProductService productService;

	@Autowired
	private UserService userService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private CartService cartService;

	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {
		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user", userDtls);
			Integer countCart = cartService.getCountCart(userDtls.getId());
			m.addAttribute("countCart", countCart);
		}

		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);
	}

	// ---------- HOME ----------
	@GetMapping("/")
	public String index(Model m) {
	    // limit categories & products for home page display
	    List<Category> allActiveCategory = categoryService.getAllActiveCategory()
	            .stream()
	            // sort by id descending (latest first)
	            .sorted(java.util.Comparator.comparing(Category::getId).reversed())
	            .limit(6)
	            .toList();

	    List<Product> allActiveProducts = productService.getAllActiveProducts("")
	            .stream()
	            // sort by id descending (latest first)
	            .sorted(java.util.Comparator.comparing(Product::getId).reversed())
	            .limit(8)
	            .toList();

	    // Use attribute name "categorys" so it matches @ModelAttribute and templates
	    m.addAttribute("categorys", allActiveCategory);
	    m.addAttribute("products", allActiveProducts);

	    return "index";
	}

	/**
	 * IMPORTANT: This must match the login page configured in SecurityConfig(). If
	 * SecurityConfig.loginPage("/login") then this mapping must be "/login".
	 */
	@GetMapping("/signin")
	public String login() {
		return "login";
	}

	/**
	 * GET register page. If redirected with flash attributes, Thymeleaf will have
	 * them in the model.
	 */
	@GetMapping("/register")
	public String register(Model model) {
		if (!model.containsAttribute("user")) {
			model.addAttribute("user", new UserDtls());
		}
		return "register";
	}

	// ---------- PRODUCTS LIST ----------
	@GetMapping("/products")
	public String products(Model m, @RequestParam(value = "category", defaultValue = "") String category,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "9") Integer pageSize, @RequestParam(defaultValue = "") String ch) {
		List<Category> categories = categoryService.getAllActiveCategory();
		m.addAttribute("paramValue", category);
		m.addAttribute("categories", categories);

		Page<Product> page = null;
		
		if (StringUtils.isEmpty(ch)) {
			page = productService.getAllActiveProductPagination(pageNo, pageSize, category);
		} else {
			page = productService.searchActiveProductPagination(pageNo, pageSize, category, ch);
		}
		
		List<Product> products = page.getContent();
		m.addAttribute("products", products);
		m.addAttribute("productsSize", products.size());
		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		// view name (you used "product" previously; change to "products" if your
		// template file is products.html)
		return "product";
	}

	// ---------- PRODUCT DETAILS ----------
	@GetMapping("/product/{id}")
	public String productDetails(@PathVariable("id") Integer id, Model m) {
		Product product = productService.getproductById(id);
		if (product == null) {
			m.addAttribute("errorMsg", "Product not found!");
			return "error";
		}

		m.addAttribute("product", product);
		return "view_product";
	}

	@GetMapping("/product")
	public String product() {
		return "view_product";
	}

	// helper to compute the base upload path
	private Path uploadBasePath() {
		return Paths.get(uploadDir).resolve("profile_img");
	}

	/**
	 * Save user and uploaded profile image.
	 *
	 * Server-side validations: - All fields required (name, mobileNumber, email,
	 * address, city, pinCode, state, password) - password == cpassword - image is
	 * required and must be an image under 5MB (per your UI)
	 *
	 * On validation failure we return to /register using RedirectAttributes (flash
	 * attributes) and re-add the submitted user object so the form is pre-filled.
	 */
	@PostMapping("/saveUser")
	public String saveUser(
	        @ModelAttribute UserDtls user,
	        @RequestParam(name = "cpassword", required = false) String cpassword,
	        @RequestParam(name = "img", required = false) MultipartFile file,
	        RedirectAttributes redirectAttrs) {

	    try {

	        /* --------------- 1) CHECK EMAIL FIRST ---------------- */
	        Boolean existsEmail = userService.existsEmail(user.getEmail());
	        if (existsEmail) {
	            redirectAttrs.addFlashAttribute("errorMsg", "Email already exists!");
	            redirectAttrs.addFlashAttribute("user", user);
	            return "redirect:/register";
	        }

	        /* --------------- 2) VALIDATE FORM FIELDS -------------- */
	        if (user == null) {
	            redirectAttrs.addFlashAttribute("errorMsg", "Form data missing.");
	            redirectAttrs.addFlashAttribute("user", user);
	            return "redirect:/register";
	        }

	        java.util.function.Predicate<String> empty = 
	                s -> (s == null || s.trim().isEmpty());

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
	            return "redirect:/register";
	        }

	        /* --------------- 3) PASSWORD MATCH CHECK -------------- */
	        if (!user.getPassword().equals(cpassword)) {
	            redirectAttrs.addFlashAttribute("errorMsg", "Password and Confirm Password do not match.");
	            redirectAttrs.addFlashAttribute("user", user);
	            return "redirect:/register";
	        }

	        /* --------------- 4) IMAGE VALIDATION ------------------ */
	        if (file == null || file.isEmpty()) {
	            redirectAttrs.addFlashAttribute("errorMsg", "Please upload a profile image.");
	            redirectAttrs.addFlashAttribute("user", user);
	            return "redirect:/register";
	        }

	        if (file.getSize() > MAX_FILE_BYTES) {
	            redirectAttrs.addFlashAttribute("errorMsg", "Profile image too large. Max allowed is 5 MB.");
	            redirectAttrs.addFlashAttribute("user", user);
	            return "redirect:/register";
	        }

	        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
	        if (!contentType.contains("image")) {
	            redirectAttrs.addFlashAttribute("errorMsg", "Invalid file type. Please upload an image.");
	            redirectAttrs.addFlashAttribute("user", user);
	            return "redirect:/register";
	        }

	        String original = StringUtils.cleanPath(file.getOriginalFilename());
	        if (original.contains("..")) {
	            redirectAttrs.addFlashAttribute("errorMsg", "Invalid file name.");
	            redirectAttrs.addFlashAttribute("user", user);
	            return "redirect:/register";
	        }

	        /* --------------- 5) SAVE IMAGE ------------------ */
	        String imageName = UUID.randomUUID() + "-" + original;
	        user.setProfileImage(imageName);

	        UserDtls savedUser = userService.saveUser(user);
	        if (savedUser == null) {
	            redirectAttrs.addFlashAttribute("errorMsg", "Something went wrong while saving user.");
	            redirectAttrs.addFlashAttribute("user", user);
	            return "redirect:/register";
	        }

	        Path base = uploadBasePath();
	        Files.createDirectories(base);
	        Path targetPath = base.resolve(imageName);
	        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

	        /* --------------- SUCCESS ------------------ */
	        redirectAttrs.addFlashAttribute("succMsg", "Registered successfully!");
	        return "redirect:/register";

	    } catch (Exception ex) {
	        ex.printStackTrace();
	        redirectAttrs.addFlashAttribute("errorMsg", "Server error: " + ex.getMessage());
	        redirectAttrs.addFlashAttribute("user", user);
	        return "redirect:/register";
	    }
	}

	// Forgot Password Code
	@GetMapping("/forgot-password")
	public String shoeForgotPassword() {
		return "forgot_password.html";
	}

	// method to send a mail if the mail exists
	@PostMapping("/forgot-password")
	public String processForgotPassword(@RequestParam String email, RedirectAttributes redirectAttrs,
			HttpServletRequest request) throws UnsupportedEncodingException, MessagingException {

		UserDtls userByEmail = userService.getUserByEmail(email);
		if (ObjectUtils.isEmpty(userByEmail)) {
			redirectAttrs.addFlashAttribute("errorMsg", "Invalid email");
		} else {
			String resetToken = UUID.randomUUID().toString();
			userService.updateUserResetToken(email, resetToken);

			// Generate Url: http://localhost:8080/reset-password?token=...
			String url = CommonUtil.generateUrl(request) + "/reset-password?token=" + resetToken;

			Boolean sendMail = commonUtil.sendMail(url, email);
			if (sendMail) {
				redirectAttrs.addFlashAttribute("succMsg", "Please check your email... Password Reset link has sent");
			} else {
				redirectAttrs.addFlashAttribute("errorMsg", "Something wrong on server! mail not sent");
			}
		}

		return "redirect:/forgot-password";
	}

	@GetMapping("/reset-password")
	public String showResetPassword(@RequestParam String token, HttpSession session, Model m) {

		UserDtls userByToken = userService.getUserByToken(token);
		if (userByToken == null) {
			m.addAttribute("msg", "Your link is invalid or expired !!");
			return "message";
		}
		m.addAttribute("token", token);
		return "reset_password.html";
	}

	@PostMapping("/reset-password")
	public String resetPassword(@RequestParam String token, @RequestParam String password,
			RedirectAttributes redirectAttrs, Model m) {

		UserDtls userByToken = userService.getUserByToken(token);
		if (userByToken == null) {
			m.addAttribute("errorMsg", "Your link is invalid or expired !!");
			return "message";
		} else {
			userByToken.setPassword(passwordEncoder.encode(password));
			userByToken.setResetToken(null);
			userService.updateUser(userByToken);
			m.addAttribute("msg", "Password Changed Successfully");
			return "message";
		}
	}

	@GetMapping("/search")
	public String searchProduct(@RequestParam String ch, Model m) {
		List<Product> searchProducts = productService.searchProduct(ch);
		m.addAttribute("products", searchProducts);

		List<Category> categories = categoryService.getAllActiveCategory();
		m.addAttribute("categories", categories);
		return "product";
	}

}
