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

		UserDtls userDtls = userRepository.findById(userId).orElse(null);
		Product product = productRepository.findById(productId).orElse(null);
		if (userDtls == null || product == null || Boolean.FALSE.equals(product.getIsActive())) {
			throw new IllegalStateException("This product is not available right now.");
		}
		if (product.getStock() <= 0) {
			throw new IllegalStateException("This product is currently out of stock.");
		}

		Cart cartStatus = cartRepository.findByProductIdAndUserId(productId, userId);

		Cart cart = null;
		double price = resolveEffectivePrice(product);

		if (ObjectUtils.isEmpty(cartStatus)) {
			cart = new Cart();
			cart.setProduct(product);
			cart.setUser(userDtls);
			cart.setQuantity(1);
			cart.setTotalPrice(price);
		} else {
			if (cartStatus.getQuantity() >= product.getStock()) {
				throw new IllegalStateException("Only " + product.getStock() + " units are available in stock.");
			}
			cart = cartStatus;
			cart.setQuantity(cart.getQuantity() + 1);
			cart.setTotalPrice(cart.getQuantity() * price);
		}
		Cart saveCart = cartRepository.save(cart);

		return saveCart;
	}

	@Override
	public List<Cart> getCartsByUser(Integer userId) {
		List<Cart> carts = cartRepository.findByUserId(userId);

		Double totalOrderPrice = 0.0;
		List<Cart> updateCarts = new ArrayList<>();
		for (Cart c : carts) {
			Double totalPrice = resolveEffectivePrice(c.getProduct()) * c.getQuantity();
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

		Cart cart = cartRepository.findById(cid).orElse(null);
		if (cart == null) {
			throw new IllegalStateException("Cart item not found.");
		}
		int updateQuantity;

		if (sy.equalsIgnoreCase("de")) {
			updateQuantity = cart.getQuantity() - 1;

			if (updateQuantity <= 0) {
				cartRepository.delete(cart);
			} else {
				cart.setQuantity(updateQuantity);
				cartRepository.save(cart);
			}

		} else {
			if (cart.getProduct().getStock() <= cart.getQuantity()) {
				throw new IllegalStateException("You have reached the available stock limit.");
			}
			updateQuantity = cart.getQuantity() + 1;
			cart.setQuantity(updateQuantity);
			cartRepository.save(cart);
		}

	}

	private double resolveEffectivePrice(Product product) {
		if (product.getDiscountPrice() != null && product.getDiscountPrice() > 0) {
			return product.getDiscountPrice();
		}
		return product.getPrice() == null ? 0.0 : product.getPrice();
	}

}
