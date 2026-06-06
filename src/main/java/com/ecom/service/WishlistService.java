package com.ecom.service;

import java.util.List;
import java.util.Set;

import com.ecom.model.WishlistItem;

public interface WishlistService {

	WishlistItem addToWishlist(Integer userId, Integer productId);

	void removeFromWishlist(Integer userId, Integer productId);

	boolean isWishlisted(Integer userId, Integer productId);

	List<WishlistItem> getWishlistByUser(Integer userId);

	Integer getWishlistCount(Integer userId);

	Set<Integer> getWishlistedProductIds(Integer userId);
}
