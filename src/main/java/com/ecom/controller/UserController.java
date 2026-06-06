package com.ecom.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ecom.model.Cart;
import com.ecom.model.Category;
import com.ecom.model.CheckoutSummary;
import com.ecom.model.OrderRequest;
import com.ecom.model.PaymentOrderResponse;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.model.WishlistItem;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.FeedbackService;
import com.ecom.service.OrderService;
import com.ecom.service.PaymentService;
import com.ecom.service.UserService;
import com.ecom.service.WishlistService;
import com.ecom.util.CheckoutCalculator;
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

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private CheckoutCalculator checkoutCalculator;

	@Autowired
	private WishlistService wishlistService;

	@Autowired
	private FeedbackService feedbackService;

	@GetMapping("/")
	public String home() {
		return "redirect:/";
	}

	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {
		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user", userDtls);
			Integer countCart = cartService.getCountCart(userDtls.getId());
			m.addAttribute("countCart", countCart);
			m.addAttribute("wishlistCount", wishlistService.getWishlistCount(userDtls.getId()));
			m.addAttribute("wishlistProductIds", wishlistService.getWishlistedProductIds(userDtls.getId()));
		}

		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);
	}

	@GetMapping("/addCart")
	public String addToCart(@RequestParam Integer pid, @RequestParam Integer uid, HttpSession session) {
		try {
			Cart saveCart = cartService.saveCart(pid, uid);
			if (ObjectUtils.isEmpty(saveCart)) {
				session.setAttribute("errorMsg", "Product add to cart failed");
			} else {
				session.setAttribute("succMsg", "Product added to cart");
			}
		} catch (Exception e) {
			session.setAttribute("errorMsg", e.getMessage());
		}
		return "redirect:/product/" + pid;
	}

	@GetMapping("/cart")
	public String loadCartPage(Principal p, Model m) {

		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		m.addAttribute("carts", carts);
		if (carts.size() > 0) {
			Double totalOrderPrice = carts.get(carts.size() - 1).getTotalOrderPrice();
			m.addAttribute("totalOrderPrice", totalOrderPrice);
		}
		return "/user/cart";
	}

	@GetMapping("/cartQuantityUpdate")
	public String updateCartQuantity(@RequestParam String sy, @RequestParam Integer cid, HttpSession session) {
		try {
			cartService.updateQuantity(sy, cid);
		} catch (Exception e) {
			session.setAttribute("errorMsg", e.getMessage());
		}
		return "redirect:/user/cart";
	}

	private UserDtls getLoggedInUserDetails(Principal p) {
		String email = p.getName();
		UserDtls userDtls = userService.getUserByEmail(email);
		return userDtls;
	}

	@GetMapping("/orders")
	public String orderPage(Principal p, Model m, HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		if (carts.isEmpty()) {
			session.setAttribute("errorMsg", "Your cart is empty");
			return "redirect:/user/cart";
		}

		CheckoutSummary checkoutSummary = checkoutCalculator.buildSummary(carts);
		m.addAttribute("carts", carts);
		m.addAttribute("orderPrice", checkoutSummary.getSubtotal());
		m.addAttribute("deliveryFee", checkoutSummary.getDeliveryFee());
		m.addAttribute("taxAmount", checkoutSummary.getTaxAmount());
		m.addAttribute("taxRatePercent", checkoutSummary.getTaxRatePercent());
		m.addAttribute("totalOrderPrice", checkoutSummary.getTotalAmount());
		m.addAttribute("onlinePaymentEnabled", paymentService.isOnlinePaymentEnabled());
		return "/user/order";
	}

	@PostMapping("/save-order")
	public String saveOrder(@ModelAttribute OrderRequest request, Principal p, HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		try {
			orderService.saveOrder(user.getId(), request);
			String paymentType = request.getPaymentType() == null ? "COD" : request.getPaymentType().trim().toUpperCase();
			session.setAttribute("paymentType", paymentType);
			session.setAttribute("orderSuccessMessage",
					"ONLINE".equals(paymentType) ? "Payment verified and order placed successfully."
							: "Order placed successfully. Pay on delivery.");

			if ("ONLINE".equals(paymentType) && request.getRazorpayPaymentId() != null) {
				session.setAttribute("paymentReference", request.getRazorpayPaymentId());
			} else {
				session.removeAttribute("paymentReference");
			}
		} catch (Exception e) {
			session.setAttribute("errorMsg", e.getMessage());
			return "redirect:/user/orders";
		}

		return "redirect:/user/success";
	}

	@PostMapping("/create-payment-order")
	@ResponseBody
	public ResponseEntity<?> createPaymentOrder(@ModelAttribute OrderRequest request, Principal p) {
		UserDtls user = getLoggedInUserDetails(p);
		try {
			PaymentOrderResponse paymentOrder = paymentService.createOrder(user, request);
			return ResponseEntity.ok(paymentOrder);
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Unable to start online payment right now. Please try again."));
		}
	}

	@GetMapping("/success")
	public String loadSuccess() {
		return "/user/success";
	}

	@GetMapping("/user-orders")
	public String myOrder(Model m, Principal p) {
		UserDtls loginUser = getLoggedInUserDetails(p);
		List<ProductOrder> orders = orderService.getOrdersByUser(loginUser.getId());
		m.addAttribute("orders", orders);
		return "/user/my_orders";
	}

	@GetMapping("/wishlist")
	public String wishlist(Model m, Principal p) {
		UserDtls loginUser = getLoggedInUserDetails(p);
		List<WishlistItem> wishlistItems = wishlistService.getWishlistByUser(loginUser.getId());
		m.addAttribute("wishlistItems", wishlistItems);
		return "/user/wishlist";
	}

	@GetMapping("/wishlist/add")
	public String addWishlist(@RequestParam Integer pid, @RequestParam(required = false) String redirect, Principal p,
			HttpSession session) {
		UserDtls loginUser = getLoggedInUserDetails(p);
		try {
			wishlistService.addToWishlist(loginUser.getId(), pid);
			session.setAttribute("succMsg", "Product added to wishlist");
		} catch (Exception e) {
			session.setAttribute("errorMsg", e.getMessage());
		}
		return safeRedirect(redirect, "/product/" + pid);
	}

	@GetMapping("/wishlist/remove")
	public String removeWishlist(@RequestParam Integer pid, @RequestParam(required = false) String redirect,
			Principal p, HttpSession session) {
		UserDtls loginUser = getLoggedInUserDetails(p);
		wishlistService.removeFromWishlist(loginUser.getId(), pid);
		session.setAttribute("succMsg", "Product removed from wishlist");
		return safeRedirect(redirect, "/user/wishlist");
	}

	@PostMapping("/feedback/save")
	public String saveFeedback(@RequestParam Integer productId, @RequestParam Integer rating,
			@RequestParam String comment, Principal p, HttpSession session) {
		UserDtls loginUser = getLoggedInUserDetails(p);
		try {
			feedbackService.saveFeedback(loginUser.getId(), productId, rating, comment);
			session.setAttribute("succMsg", "Your rating and feedback have been saved");
		} catch (Exception e) {
			session.setAttribute("errorMsg", e.getMessage());
		}
		return "redirect:/product/" + productId;
	}

	@GetMapping("/receipt/{id}")
	public ResponseEntity<byte[]> downloadReceipt(@PathVariable Integer id, Principal p) {
		UserDtls loginUser = getLoggedInUserDetails(p);
		return orderService.downloadReceipt(id, loginUser.getId());
	}

	@GetMapping("/update-status")
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
		return "redirect:/user/user-orders";
	}

	@GetMapping("/profile")
	public String profile() {
		return "/user/profile";
	}

	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img, HttpSession session) {
		UserDtls updateUserProfile = userService.updateUserProfile(user, img);
		if (ObjectUtils.isEmpty(updateUserProfile)) {
			session.setAttribute("errorMsg", "Profile not updated");
		} else {
			session.setAttribute("succMsg", "Profile Updated");
		}
		return "redirect:/user/profile";
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam String newPassword, @RequestParam String currentPassword, Principal p,
			HttpSession session) {
		UserDtls loggedInUserDetails = getLoggedInUserDetails(p);

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

		return "redirect:/user/profile";
	}

	private String safeRedirect(String redirect, String fallback) {
		if (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")) {
			return "redirect:" + redirect;
		}
		return "redirect:" + fallback;
	}

}
