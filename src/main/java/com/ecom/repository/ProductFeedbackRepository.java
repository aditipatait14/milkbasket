package com.ecom.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecom.model.ProductFeedback;

import jakarta.transaction.Transactional;

public interface ProductFeedbackRepository extends JpaRepository<ProductFeedback, Integer> {

	List<ProductFeedback> findByProductIdOrderByUpdatedAtDesc(Integer productId);

	List<ProductFeedback> findAllByOrderByUpdatedAtDesc();

	Optional<ProductFeedback> findByProductIdAndUserId(Integer productId, Integer userId);

	long countByProductId(Integer productId);

	boolean existsByProductId(Integer productId);

	@Transactional
	void deleteByProductId(Integer productId);

	@Query("select coalesce(avg(f.rating), 0) from ProductFeedback f where f.product.id = :productId")
	Double getAverageRatingByProductId(@Param("productId") Integer productId);
}
