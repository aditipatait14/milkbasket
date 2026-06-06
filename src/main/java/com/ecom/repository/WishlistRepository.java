package com.ecom.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.model.WishlistItem;

import jakarta.transaction.Transactional;

public interface WishlistRepository extends JpaRepository<WishlistItem, Integer> {

	Optional<WishlistItem> findByUserIdAndProductId(Integer userId, Integer productId);

	List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Integer userId);

	Integer countByUserId(Integer userId);

	List<WishlistItem> findAllByUserId(Integer userId);

	boolean existsByProductId(Integer productId);

	@Transactional
	void deleteByProductId(Integer productId);
}
