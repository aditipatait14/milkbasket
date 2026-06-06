package com.ecom.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Category;
import com.ecom.model.ContactMessage;
import com.ecom.model.Product;
import com.ecom.model.ProductFeedback;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.ContactMessageService;
import com.ecom.service.FeedbackService;
import com.ecom.service.FileStorageService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.service.WishlistService;
import com.ecom.util.CommonUtil;

import io.micrometer.common.util.StringUtils;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

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

	@Autowired
	private FileStorageService fileStorageService;

	@Autowired
	private ContactMessageService contactMessageService;

	@Autowired
	private WishlistService wishlistService;

	@Autowired
	private FeedbackService feedbackService;

	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {
		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user", userDtls);
			Integer countCart = cartService.getCountCart(userDtls.getId());
			m.addAttribute("countCart", countCart);
			if ("ROLE_USER".equals(userDtls.getRole())) {
				m.addAttribute("wishlistCount", wishlistService.getWishlistCount(userDtls.getId()));
				m.addAttribute("wishlistProductIds", wishlistService.getWishlistedProductIds(userDtls.getId()));
			}
		}

		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);
	}

	@GetMapping("/")
	public String index(Model m) {

		List<Category> allActiveCategory = categoryService.getAllActiveCategory().stream()
				.sorted((c1, c2) -> c2.getId().compareTo(c1.getId())).limit(6).toList();
		List<Product> allActiveProducts = productService.getAllActiveProducts("").stream()
				.sorted((p1, p2) -> p2.getId().compareTo(p1.getId())).limit(8).toList();
		m.addAttribute("category", allActiveCategory);
		m.addAttribute("products", allActiveProducts);
		return "index";
	}

	@GetMapping("/signin")
	public String login() {
		return "login";
	}

	@GetMapping("/register")
	public String register() {
		return "register";
	}

	@GetMapping("/contact")
	public String contact() {
		return "contact";
	}

	@GetMapping("/access-denied")
	public String accessDenied() {
		return "access_denied";
	}

	@GetMapping("/products")
	public String products(Model m, @RequestParam(value = "category", defaultValue = "") String category,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "12") Integer pageSize,
			@RequestParam(defaultValue = "") String ch) {

		String normalizedCategory = category == null ? "" : category.trim();
		String normalizedSearch = ch == null ? "" : ch.trim();
		List<Category> categories = categoryService.getAllActiveCategory();
		m.addAttribute("paramValue", normalizedCategory);
		m.addAttribute("categories", categories);
		m.addAttribute("searchValue", normalizedSearch);

//		List<Product> products = productService.getAllActiveProducts(category);
//		m.addAttribute("products", products);
		Page<Product> page = null;
		if (StringUtils.isEmpty(normalizedSearch)) {
			page = productService.getAllActiveProductPagination(pageNo, pageSize, normalizedCategory);
		} else {
			page = productService.searchActiveProductPagination(pageNo, pageSize, normalizedCategory, normalizedSearch);
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

		return "product";
	}

	@GetMapping("/product/{id}")
	public String product(@PathVariable int id, Model m, Principal principal, HttpSession session) {
		Product productById = productService.getProductById(id);
		if (productById == null) {
			session.setAttribute("errorMsg", "Product not found");
			return "redirect:/products";
		}
		List<ProductFeedback> feedbacks = feedbackService.getFeedbacksForProduct(id);
		m.addAttribute("product", productById);
		m.addAttribute("feedbacks", feedbacks);
		if (principal != null) {
			UserDtls user = userService.getUserByEmail(principal.getName());
			if (user != null && "ROLE_USER".equals(user.getRole())) {
				m.addAttribute("existingFeedback", feedbackService.getUserFeedbackForProduct(user.getId(), id));
			}
		}
		return "view_product";
	}

	@PostMapping("/saveUser")
	public String saveUser(@ModelAttribute UserDtls user, @RequestParam("img") MultipartFile file, HttpSession session)
			throws IOException {

		Boolean existsEmail = userService.existsEmail(user.getEmail());

		if (existsEmail) {
			session.setAttribute("errorMsg", "Email already exist");
		} else {
			String imageName = file.isEmpty() ? "ecom.png" : fileStorageService.storeProfileImage(file);
			user.setProfileImage(imageName);
			UserDtls saveUser = userService.saveUser(user);

			if (!ObjectUtils.isEmpty(saveUser)) {
				session.setAttribute("succMsg", "Register successfully");
			} else {
				session.setAttribute("errorMsg", "something wrong on server");
			}
		}

		return "redirect:/register";
	}

//	Forgot Password Code 

	@GetMapping("/forgot-password")
	public String showForgotPassword() {
		return "forgot_password.html";
	}

	@PostMapping("/forgot-password")
	public String processForgotPassword(@RequestParam String email, HttpSession session, HttpServletRequest request)
			throws UnsupportedEncodingException, MessagingException {

		UserDtls userByEmail = userService.getUserByEmail(email);

		if (ObjectUtils.isEmpty(userByEmail)) {
			session.setAttribute("errorMsg", "Invalid email");
		} else {

			String resetToken = UUID.randomUUID().toString();
			userService.updateUserResetToken(email, resetToken);

			// Generate URL :
			// http://localhost:8080/reset-password?token=sfgdbgfswegfbdgfewgvsrg

			String url = CommonUtil.generateUrl(request) + "/reset-password?token=" + resetToken;

			Boolean sendMail = commonUtil.sendMail(url, email);

			if (sendMail) {
				session.setAttribute("succMsg", "Please check your email..Password Reset link sent");
			} else {
				session.setAttribute("errorMsg", "Somethong wrong on server ! Email not send");
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
		return "reset_password";
	}

	@PostMapping("/reset-password")
	public String resetPassword(@RequestParam String token, @RequestParam String password, HttpSession session,
			Model m) {

		UserDtls userByToken = userService.getUserByToken(token);
		if (userByToken == null) {
			m.addAttribute("errorMsg", "Your link is invalid or expired !!");
			return "message";
		} else {
			userByToken.setPassword(passwordEncoder.encode(password));
			userByToken.setResetToken(null);
			userService.updateUser(userByToken);
			// session.setAttribute("succMsg", "Password change successfully");
			m.addAttribute("msg", "Password change successfully");

			return "message";
		}

	}

	@GetMapping("/search")
	public String searchProduct(@RequestParam String ch, Model m) {
		String normalizedSearch = ch == null ? "" : ch.trim();
		List<Product> searchProducts = productService.searchProduct(normalizedSearch);
		m.addAttribute("products", searchProducts);
		m.addAttribute("productsSize", searchProducts.size());
		List<Category> categories = categoryService.getAllActiveCategory();
		m.addAttribute("categories", categories);
		m.addAttribute("paramValue", "");
		m.addAttribute("searchValue", normalizedSearch);
		m.addAttribute("pageNo", 0);
		m.addAttribute("pageSize", searchProducts.size());
		m.addAttribute("totalElements", searchProducts.size());
		m.addAttribute("totalPages", searchProducts.isEmpty() ? 0 : 1);
		m.addAttribute("isFirst", true);
		m.addAttribute("isLast", true);
		return "product";

	}

	@PostMapping("/contact")
	public String saveContactMessage(@ModelAttribute ContactMessage contactMessage, HttpSession session) {
		contactMessageService.save(contactMessage);
		session.setAttribute("succMsg", "Thanks for reaching out. Our team will contact you shortly.");
		return "redirect:/contact";
	}

}
