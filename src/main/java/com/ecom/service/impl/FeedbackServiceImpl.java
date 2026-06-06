package com.ecom.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecom.model.Product;
import com.ecom.model.ProductFeedback;
import com.ecom.model.UserDtls;
import com.ecom.repository.ProductFeedbackRepository;
import com.ecom.repository.ProductRepository;
import com.ecom.repository.UserRepository;
import com.ecom.service.FeedbackService;

@Service
public class FeedbackServiceImpl implements FeedbackService {

	@Autowired
	private ProductFeedbackRepository productFeedbackRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private UserRepository userRepository;

	@Override
	public ProductFeedback saveFeedback(Integer userId, Integer productId, Integer rating, String comment) {
		if (rating == null || rating < 1 || rating > 5) {
			throw new IllegalStateException("Please select a rating between 1 and 5 stars.");
		}

		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new IllegalStateException("Product not found."));
		UserDtls user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalStateException("User account not found."));

		ProductFeedback feedback = productFeedbackRepository.findByProductIdAndUserId(productId, userId).orElseGet(() -> {
			ProductFeedback created = new ProductFeedback();
			created.setCreatedAt(LocalDateTime.now());
			created.setProduct(product);
			created.setUser(user);
			return created;
		});

		feedback.setRating(rating);
		feedback.setComment(comment == null ? "" : comment.trim());
		feedback.setUpdatedAt(LocalDateTime.now());
		ProductFeedback savedFeedback = productFeedbackRepository.save(feedback);
		refreshProductRating(product);
		return savedFeedback;
	}

	@Override
	public List<ProductFeedback> getFeedbacksForProduct(Integer productId) {
		return productFeedbackRepository.findByProductIdOrderByUpdatedAtDesc(productId);
	}

	@Override
	public List<ProductFeedback> getAllFeedbacks() {
		return productFeedbackRepository.findAllByOrderByUpdatedAtDesc();
	}

	@Override
	public ProductFeedback getUserFeedbackForProduct(Integer userId, Integer productId) {
		return productFeedbackRepository.findByProductIdAndUserId(productId, userId).orElse(null);
	}

	private void refreshProductRating(Product product) {
		Double averageRating = productFeedbackRepository.getAverageRatingByProductId(product.getId());
		long ratingCount = productFeedbackRepository.countByProductId(product.getId());
		product.setAverageRating(round(averageRating == null ? 0.0 : averageRating));
		product.setRatingCount((int) ratingCount);
		productRepository.save(product);
	}

	private double round(double value) {
		return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
	}
}
