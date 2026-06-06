package com.ecom.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Category;
import com.ecom.model.ContactMessage;
import com.ecom.model.AdminAnalytics;
import com.ecom.model.Product;
import com.ecom.model.ProductFeedback;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.service.AnalyticsService;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.ContactMessageService;
import com.ecom.service.FeedbackService;
import com.ecom.service.FileStorageService;
import com.ecom.service.OrderService;
import com.ecom.service.ProductService;
import com.ecom.service.StoreSettingService;
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

	@Autowired
	private FileStorageService fileStorageService;

	@Autowired
	private ContactMessageService contactMessageService;

	@Autowired
	private StoreSettingService storeSettingService;

	@Autowired
	private FeedbackService feedbackService;

	@Autowired
	private AnalyticsService analyticsService;

	private static final int LOW_STOCK_THRESHOLD = 7;

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

	@GetMapping("/")
	public String index(Model m) {
		AdminAnalytics analytics = analyticsService.buildAdminAnalytics();
		m.addAttribute("analytics", analytics);
		m.addAttribute("lowStockThreshold", LOW_STOCK_THRESHOLD);
		return "admin/index";
	}

	@GetMapping("/loadAddProduct")
	public String loadAddProduct(Model m) {
		List<Category> categories = categoryService.getAllCategory();
		m.addAttribute("categories", categories);
		return "admin/add_product";
	}

	@GetMapping("/category")
	public String category(Model m, @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
		// m.addAttribute("categorys", categoryService.getAllCategory());
		Page<Category> page = categoryService.getAllCategorPagination(pageNo, pageSize);
		List<Category> categorys = page.getContent();
		m.addAttribute("categorys", categorys);

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "admin/category";
	}

	@PostMapping("/saveCategory")
	public String saveCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,
			HttpSession session) throws IOException {
		category.setName(category.getName().trim());

		Boolean existCategory = categoryService.existCategory(category.getName());

		if (existCategory) {
			session.setAttribute("errorMsg", "Category Name already exists");
		} else {
			String imageName = file != null && !file.isEmpty() ? fileStorageService.storeCategoryImage(file) : "ecom.png";
			category.setImageName(imageName);

			Category saveCategory = categoryService.saveCategory(category);

			if (ObjectUtils.isEmpty(saveCategory)) {
				session.setAttribute("errorMsg", "Not saved ! internal server error");
			} else {
				session.setAttribute("succMsg", "Saved successfully");
			}
		}

		return "redirect:/admin/category";
	}

	@GetMapping("/deleteCategory/{id}")
	public String deleteCategory(@PathVariable int id, HttpSession session) {
		Boolean deleteCategory = categoryService.deleteCategory(id);

		if (deleteCategory) {
			session.setAttribute("succMsg", "category delete success");
		} else {
			session.setAttribute("errorMsg", "something wrong on server");
		}

		return "redirect:/admin/category";
	}

	@GetMapping("/loadEditCategory/{id}")
	public String loadEditCategory(@PathVariable int id, Model m) {
		m.addAttribute("category", categoryService.getCategoryById(id));
		return "admin/edit_category";
	}

	@PostMapping("/updateCategory")
	public String updateCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,
			HttpSession session) throws IOException {

		Category oldCategory = categoryService.getCategoryById(category.getId());
		if (oldCategory == null) {
			session.setAttribute("errorMsg", "Category not found");
			return "redirect:/admin/category";
		}
		String imageName = oldCategory.getImageName();
		if (file != null && !file.isEmpty()) {
			imageName = fileStorageService.storeCategoryImage(file);
			fileStorageService.deleteCategoryImage(oldCategory.getImageName());
		}
		String previousCategoryName = oldCategory.getName();

		if (!ObjectUtils.isEmpty(category)) {

			oldCategory.setName(category.getName().trim());
			oldCategory.setIsActive(category.getIsActive());
			oldCategory.setImageName(imageName);
		}

		Category updateCategory = categoryService.saveCategory(oldCategory);

		if (!ObjectUtils.isEmpty(updateCategory)) {
			productService.syncCategoryName(previousCategoryName, updateCategory.getName());
			session.setAttribute("succMsg", "Category update success");
		} else {
			session.setAttribute("errorMsg", "something wrong on server");
		}

		return "redirect:/admin/loadEditCategory/" + category.getId();
	}

	@PostMapping("/saveProduct")
	public String saveProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile image,
			HttpSession session) throws IOException {

		String imageName = image.isEmpty() ? "ecom.png" : fileStorageService.storeProductImage(image);

		product.setImage(imageName);
		product.setTitle(product.getTitle() == null ? null : product.getTitle().trim());
		product.setCategory(product.getCategory() == null ? null : product.getCategory().trim());
		if (product.getDiscount() < 0 || product.getDiscount() > 100) {
			session.setAttribute("errorMsg", "Discount must be between 0 and 100");
			return "redirect:/admin/loadAddProduct";
		}
		if (product.getCostPrice() == null || product.getCostPrice() < 0) {
			session.setAttribute("errorMsg", "Cost price must be 0 or more");
			return "redirect:/admin/loadAddProduct";
		}
		Product saveProduct = productService.saveProduct(product);

		if (!ObjectUtils.isEmpty(saveProduct)) {
			session.setAttribute("succMsg", "Product Saved Success");
		} else {
			session.setAttribute("errorMsg", "something wrong on server");
		}

		return "redirect:/admin/loadAddProduct";
	}

	@GetMapping("/products")
	public String loadViewProduct(Model m, @RequestParam(defaultValue = "") String ch,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {

//		List<Product> products = null;
//		if (ch != null && ch.length() > 0) {
//			products = productService.searchProduct(ch);
//		} else {
//			products = productService.getAllProducts();
//		}
//		m.addAttribute("products", products);

		Page<Product> page = null;
		if (ch != null && ch.length() > 0) {
			page = productService.searchProductPagination(pageNo, pageSize, ch);
		} else {
			page = productService.getAllProductsPagination(pageNo, pageSize);
		}
		m.addAttribute("products", page.getContent());
		m.addAttribute("lowStockThreshold", LOW_STOCK_THRESHOLD);

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "admin/products";
	}

	@GetMapping("/deleteProduct/{id}")
	public String deleteProduct(@PathVariable int id, HttpSession session) {
		try {
			Boolean deleteProduct = productService.deleteProduct(id);
			if (deleteProduct) {
				session.setAttribute("succMsg", "Product removed successfully. If the product had existing orders, it was archived from the storefront.");
			} else {
				session.setAttribute("errorMsg", "Product not found");
			}
		} catch (Exception e) {
			session.setAttribute("errorMsg", "Unable to delete product right now");
		}
		return "redirect:/admin/products";
	}

	@GetMapping("/editProduct/{id}")
	public String editProduct(@PathVariable int id, Model m, HttpSession session) {
		Product product = productService.getProductById(id);
		if (product == null) {
			session.setAttribute("errorMsg", "Product not found");
			return "redirect:/admin/products";
		}
		m.addAttribute("product", product);
		m.addAttribute("categories", categoryService.getAllCategory());
		return "admin/edit_product";
	}

	@PostMapping("/updateProduct")
	public String updateProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile image,
			HttpSession session, Model m) {

		if (product.getDiscount() < 0 || product.getDiscount() > 100) {
			session.setAttribute("errorMsg", "invalid Discount");
		} else if (product.getCostPrice() == null || product.getCostPrice() < 0) {
			session.setAttribute("errorMsg", "Cost price must be 0 or more");
		} else {
			Product updateProduct = productService.updateProduct(product, image);
			if (!ObjectUtils.isEmpty(updateProduct)) {
				session.setAttribute("succMsg", "Product update success");
			} else {
				session.setAttribute("errorMsg", "Something wrong on server");
			}
		}
		return "redirect:/admin/editProduct/" + product.getId();
	}

	@PostMapping("/update-stock")
	public String updateStock(@RequestParam Integer productId, @RequestParam Integer stock, HttpSession session) {
		if (stock == null || stock < 0) {
			session.setAttribute("errorMsg", "Stock must be 0 or more");
			return "redirect:/admin/products";
		}

		Product updatedProduct = productService.updateStock(productId, stock);
		if (updatedProduct == null) {
			session.setAttribute("errorMsg", "Product not found");
		} else {
			session.setAttribute("succMsg", "Stock updated for " + updatedProduct.getTitle());
		}
		return "redirect:/admin/products";
	}

	@GetMapping("/users")
	public String getAllUsers(Model m, @RequestParam Integer type) {
		List<UserDtls> users = null;
		if (type == 1) {
			users = userService.getUsers("ROLE_USER");
		} else {
			users = userService.getUsers("ROLE_ADMIN");
		}
		m.addAttribute("userType",type);
		m.addAttribute("users", users);
		return "/admin/users";
	}

	@GetMapping("/updateSts")
	public String updateUserAccountStatus(@RequestParam Boolean status, @RequestParam Integer id,@RequestParam Integer type, HttpSession session) {
		Boolean f = userService.updateAccountStatus(id, status);
		if (f) {
			session.setAttribute("succMsg", "Account Status Updated");
		} else {
			session.setAttribute("errorMsg", "Something wrong on server");
		}
		return "redirect:/admin/users?type="+type;
	}

	@GetMapping("/orders")
	public String getAllOrders(Model m, @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
//		List<ProductOrder> allOrders = orderService.getAllOrders();
//		m.addAttribute("orders", allOrders);
//		m.addAttribute("srch", false);

		Page<ProductOrder> page = orderService.getAllOrdersPagination(pageNo, pageSize);
		m.addAttribute("orders", page.getContent());
		m.addAttribute("srch", false);

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "/admin/orders";
	}

	@GetMapping("/contacts")
	public String contactMessages(Model m) {
		List<ContactMessage> contacts = contactMessageService.getAll();
		m.addAttribute("contacts", contacts);
		return "/admin/contacts";
	}

	@GetMapping("/feedbacks")
	public String feedbacks(Model m) {
		List<ProductFeedback> feedbacks = feedbackService.getAllFeedbacks();
		m.addAttribute("feedbacks", feedbacks);
		return "/admin/feedbacks";
	}

	@GetMapping("/analytics")
	public String analytics(Model m) {
		m.addAttribute("analytics", analyticsService.buildAdminAnalytics());
		m.addAttribute("lowStockThreshold", LOW_STOCK_THRESHOLD);
		return "/admin/analytics";
	}

	@GetMapping("/settings")
	public String settings(Model m) {
		m.addAttribute("storeSettings", storeSettingService.getSettings());
		return "/admin/settings";
	}

	@PostMapping("/settings")
	public String updateSettings(@RequestParam Double deliveryFee, @RequestParam Double taxRatePercent,
			HttpSession session) {
		if (deliveryFee == null || deliveryFee < 0) {
			session.setAttribute("errorMsg", "Delivery charge must be 0 or more");
			return "redirect:/admin/settings";
		}
		if (taxRatePercent == null || taxRatePercent < 0 || taxRatePercent > 5) {
			session.setAttribute("errorMsg", "Tax rate for dairy checkout must stay between 0% and 5%");
			return "redirect:/admin/settings";
		}

		storeSettingService.saveSettings(deliveryFee, taxRatePercent);
		session.setAttribute("succMsg", "Checkout settings updated successfully");
		return "redirect:/admin/settings";
	}

	@GetMapping("/contacts/{id}")
	public String contactDetails(@PathVariable Integer id, Model m, HttpSession session) {
		ContactMessage contactMessage = contactMessageService.markAsViewed(id);
		if (contactMessage == null) {
			session.setAttribute("errorMsg", "Contact message not found");
			return "redirect:/admin/contacts";
		}
		m.addAttribute("contact", contactMessage);
		return "/admin/contact_details";
	}

	@PostMapping("/contacts/respond")
	public String respondToContact(@RequestParam Integer id, @RequestParam String responseMessage, HttpSession session) {
		ContactMessage contactMessage = contactMessageService.getById(id);
		if (contactMessage == null) {
			session.setAttribute("errorMsg", "Contact message not found");
			return "redirect:/admin/contacts";
		}

		try {
			commonUtil.sendContactReply(contactMessage, responseMessage);
			contactMessageService.saveResponse(id, responseMessage);
			session.setAttribute("succMsg", "Reply sent successfully");
		} catch (Exception e) {
			session.setAttribute("errorMsg", "Unable to send reply right now");
		}
		return "redirect:/admin/contacts/" + id;
	}

	@GetMapping("/contacts/delete/{id}")
	public String deleteContact(@PathVariable Integer id, HttpSession session) {
		ContactMessage contactMessage = contactMessageService.getById(id);
		if (contactMessage == null) {
			session.setAttribute("errorMsg", "Contact message not found");
		} else {
			contactMessageService.deleteById(id);
			session.setAttribute("succMsg", "Contact message deleted");
		}
		return "redirect:/admin/contacts";
	}

	@PostMapping("/update-order-status")
	public String updateOrderStatus(@RequestParam Integer id, @RequestParam Integer st, HttpSession session) {

		OrderStatus[] values = OrderStatus.values();
		String status = null;

		for (OrderStatus orderSt : values) {
			if (orderSt.getId().equals(st)) {
				status = orderSt.getName();
			}
		}

		ProductOrder updateOrder = orderService.updateOrderStatus(id, status);

		try {
			if (updateOrder != null) {
				commonUtil.sendMailForProductOrder(updateOrder, status);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!ObjectUtils.isEmpty(updateOrder)) {
			session.setAttribute("succMsg", "Status Updated");
		} else {
			session.setAttribute("errorMsg", "status not updated");
		}
		return "redirect:/admin/orders";
	}

	@GetMapping("/search-order")
	public String searchProduct(@RequestParam String orderId, Model m, HttpSession session,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {

		if (orderId != null && orderId.length() > 0) {

			ProductOrder order = orderService.getOrdersByOrderId(orderId.trim());

			if (ObjectUtils.isEmpty(order)) {
				session.setAttribute("errorMsg", "Incorrect orderId");
				m.addAttribute("orderDtls", null);
			} else {
				m.addAttribute("orderDtls", order);
			}

			m.addAttribute("srch", true);
		} else {
//			List<ProductOrder> allOrders = orderService.getAllOrders();
//			m.addAttribute("orders", allOrders);
//			m.addAttribute("srch", false);

			Page<ProductOrder> page = orderService.getAllOrdersPagination(pageNo, pageSize);
			m.addAttribute("orders", page.getContent());
			m.addAttribute("srch", false);

			m.addAttribute("pageNo", page.getNumber());
			m.addAttribute("pageSize", pageSize);
			m.addAttribute("totalElements", page.getTotalElements());
			m.addAttribute("totalPages", page.getTotalPages());
			m.addAttribute("isFirst", page.isFirst());
			m.addAttribute("isLast", page.isLast());

		}
		return "/admin/orders";

	}

	@GetMapping("/add-admin")
	public String loadAdminAdd() {
		return "/admin/add_admin";
	}

	@PostMapping("/save-admin")
	public String saveAdmin(@ModelAttribute UserDtls user, @RequestParam("img") MultipartFile file, HttpSession session)
			throws IOException {

		String imageName = file.isEmpty() ? "ecom.png" : fileStorageService.storeProfileImage(file);
		user.setProfileImage(imageName);
		UserDtls saveUser = userService.saveAdmin(user);

		if (!ObjectUtils.isEmpty(saveUser)) {
			session.setAttribute("succMsg", "Register successfully");
		} else {
			session.setAttribute("errorMsg", "something wrong on server");
		}

		return "redirect:/admin/add-admin";
	}

	@GetMapping("/profile")
	public String profile() {
		return "/admin/profile";
	}

	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img, HttpSession session) {
		UserDtls updateUserProfile = userService.updateUserProfile(user, img);
		if (ObjectUtils.isEmpty(updateUserProfile)) {
			session.setAttribute("errorMsg", "Profile not updated");
		} else {
			session.setAttribute("succMsg", "Profile Updated");
		}
		return "redirect:/admin/profile";
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam String newPassword, @RequestParam String currentPassword, Principal p,
			HttpSession session) {
		UserDtls loggedInUserDetails = commonUtil.getLoggedInUserDetails(p);

		boolean matches = passwordEncoder.matches(currentPassword, loggedInUserDetails.getPassword());

		if (matches) {
			String encodePassword = passwordEncoder.encode(newPassword);
			loggedInUserDetails.setPassword(encodePassword);
			UserDtls updateUser = userService.updateUser(loggedInUserDetails);
			if (ObjectUtils.isEmpty(updateUser)) {
				session.setAttribute("errorMsg", "Password not updated !! Error in server");
			} else {
				session.setAttribute("succMsg", "Password Updated sucessfully");
			}
		} else {
			session.setAttribute("errorMsg", "Current Password incorrect");
		}

		return "redirect:/admin/profile";
	}

}
