package com.ecom.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecom.model.Product;
import com.ecom.model.UserDtls;
import com.ecom.model.WishlistItem;
import com.ecom.repository.ProductRepository;
import com.ecom.repository.UserRepository;
import com.ecom.repository.WishlistRepository;
import com.ecom.service.WishlistService;

@Service
public class WishlistServiceImpl implements WishlistService {

	@Autowired
	private WishlistRepository wishlistRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	@Override
	public WishlistItem addToWishlist(Integer userId, Integer productId) {
		WishlistItem existingItem = wishlistRepository.findByUserIdAndProductId(userId, productId).orElse(null);
		if (existingItem != null) {
			return existingItem;
		}

		UserDtls user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalStateException("User account not found."));
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new IllegalStateException("Product not found."));
		if (Boolean.FALSE.equals(product.getIsActive())) {
			throw new IllegalStateException("This product is not available right now.");
		}

		WishlistItem wishlistItem = new WishlistItem();
		wishlistItem.setUser(user);
		wishlistItem.setProduct(product);
		wishlistItem.setCreatedAt(LocalDateTime.now());
		return wishlistRepository.save(wishlistItem);
	}

	@Override
	public void removeFromWishlist(Integer userId, Integer productId) {
		wishlistRepository.findByUserIdAndProductId(userId, productId).ifPresent(wishlistRepository::delete);
	}

	@Override
	public boolean isWishlisted(Integer userId, Integer productId) {
		return wishlistRepository.findByUserIdAndProductId(userId, productId).isPresent();
	}

	@Override
	public List<WishlistItem> getWishlistByUser(Integer userId) {
		return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
	}

	@Override
	public Integer getWishlistCount(Integer userId) {
		Integer count = wishlistRepository.countByUserId(userId);
		return count == null ? 0 : count;
	}

	@Override
	public Set<Integer> getWishlistedProductIds(Integer userId) {
		return wishlistRepository.findAllByUserId(userId).stream().map(item -> item.getProduct().getId())
				.collect(Collectors.toSet());
	}
}
