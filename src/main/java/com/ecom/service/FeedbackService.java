package com.ecom.service;

import java.util.List;

import com.ecom.model.ProductFeedback;

public interface FeedbackService {

	ProductFeedback saveFeedback(Integer userId, Integer productId, Integer rating, String comment);

	List<ProductFeedback> getFeedbacksForProduct(Integer productId);

	List<ProductFeedback> getAllFeedbacks();

	ProductFeedback getUserFeedbackForProduct(Integer userId, Integer productId);
}
