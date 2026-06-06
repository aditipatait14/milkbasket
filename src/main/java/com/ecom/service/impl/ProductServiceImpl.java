package com.ecom.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Product;
import com.ecom.repository.CartRepository;
import com.ecom.repository.ProductFeedbackRepository;
import com.ecom.repository.ProductOrderRepository;
import com.ecom.repository.ProductRepository;
import com.ecom.repository.WishlistRepository;
import com.ecom.service.ProductService;
import com.ecom.service.FileStorageService;

@Service
public class ProductServiceImpl implements ProductService {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private FileStorageService fileStorageService;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private ProductOrderRepository productOrderRepository;

	@Autowired
	private WishlistRepository wishlistRepository;

	@Autowired
	private ProductFeedbackRepository productFeedbackRepository;

	@Override
	public Product saveProduct(Product product) {
		product.setTitle(product.getTitle() == null ? null : product.getTitle().trim());
		product.setCategory(product.getCategory() == null ? null : product.getCategory().trim());
		applyDefaultValues(product);
		applyDiscountPricing(product);
		return productRepository.save(product);
	}

	@Override
	public List<Product> getAllProducts() {
		return productRepository.findAll();
	}

	@Override
	public Page<Product> getAllProductsPagination(Integer pageNo, Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return productRepository.findAll(pageable);
	}

	@Override
	@Transactional
	public Boolean deleteProduct(Integer id) {
		Product product = productRepository.findById(id).orElse(null);

		if (!ObjectUtils.isEmpty(product)) {
			if (productOrderRepository.existsByProductId(id)) {
				product.setIsActive(false);
				product.setStock(0);
				productRepository.save(product);
				if (cartRepository.existsByProductId(id)) {
					cartRepository.deleteByProductId(id);
				}
				if (wishlistRepository.existsByProductId(id)) {
					wishlistRepository.deleteByProductId(id);
				}
				return true;
			}
			if (cartRepository.existsByProductId(id)) {
				cartRepository.deleteByProductId(id);
			}
			if (wishlistRepository.existsByProductId(id)) {
				wishlistRepository.deleteByProductId(id);
			}
			if (productFeedbackRepository.existsByProductId(id)) {
				productFeedbackRepository.deleteByProductId(id);
			}
			fileStorageService.deleteProductImage(product.getImage());
			productRepository.delete(product);
			return true;
		}
		return false;
	}

	@Override
	public Product getProductById(Integer id) {
		Product product = productRepository.findById(id).orElse(null);
		return product;
	}

	@Override
	public Product updateProduct(Product product, MultipartFile image) {

		Product dbProduct = getProductById(product.getId());
		if (dbProduct == null) {
			return null;
		}

		String imageName = dbProduct.getImage();
		if (image != null && !image.isEmpty()) {
			imageName = fileStorageService.storeProductImage(image);
			fileStorageService.deleteProductImage(dbProduct.getImage());
		}

		dbProduct.setTitle(product.getTitle() == null ? null : product.getTitle().trim());
		dbProduct.setDescription(product.getDescription() == null ? null : product.getDescription().trim());
		dbProduct.setCategory(product.getCategory() == null ? null : product.getCategory().trim());
		dbProduct.setPrice(product.getPrice());
		dbProduct.setCostPrice(product.getCostPrice());
		dbProduct.setStock(product.getStock());
		dbProduct.setImage(imageName);
		dbProduct.setIsActive(product.getIsActive());
		dbProduct.setDiscount(product.getDiscount());
		applyDefaultValues(dbProduct);
		applyDiscountPricing(dbProduct);

		Product updateProduct = productRepository.save(dbProduct);

		if (!ObjectUtils.isEmpty(updateProduct)) {
			return updateProduct;
		}
		return null;
	}

	@Override
	public List<Product> getAllActiveProducts(String category) {
		List<Product> products = null;
		if (ObjectUtils.isEmpty(category)) {
			products = productRepository.findByIsActiveTrue();
		} else {
			products = productRepository.findActiveByCategoryNormalized(category.trim());
		}

		return products;
	}

	@Override
	public List<Product> searchProduct(String ch) {
		return productRepository.searchCatalog(ch == null ? "" : ch.trim());
	}

	@Override
	public Page<Product> searchProductPagination(Integer pageNo, Integer pageSize, String ch) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return productRepository.searchCatalog(ch == null ? "" : ch.trim(), pageable);
	}

	@Override
	public Page<Product> getAllActiveProductPagination(Integer pageNo, Integer pageSize, String category) {

		Pageable pageable = PageRequest.of(pageNo, pageSize);
		Page<Product> pageProduct = null;

		if (ObjectUtils.isEmpty(category)) {
			pageProduct = productRepository.findByIsActiveTrue(pageable);
		} else {
			pageProduct = productRepository.findActiveByCategoryNormalized(category.trim(), pageable);
		}
		return pageProduct;
	}

	@Override
	public Page<Product> searchActiveProductPagination(Integer pageNo, Integer pageSize, String category, String ch) {

		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return productRepository.searchActiveCatalog(category == null ? "" : category.trim(),
				ch == null ? "" : ch.trim(), pageable);
	}

	@Override
	public void syncCategoryName(String oldCategory, String newCategory) {
		if (oldCategory == null || newCategory == null) {
			return;
		}

		String existingCategory = oldCategory.trim();
		String updatedCategory = newCategory.trim();
		if (existingCategory.isEmpty() || updatedCategory.isEmpty()
				|| existingCategory.equalsIgnoreCase(updatedCategory)) {
			return;
		}

		productRepository.updateCategoryName(existingCategory, updatedCategory);
	}

	@Override
	public Product updateStock(Integer productId, Integer stock) {
		Product product = getProductById(productId);
		if (product == null) {
			return null;
		}
		product.setStock(Math.max(stock == null ? 0 : stock, 0));
		return productRepository.save(product);
	}

	@Override
	public List<Product> getLowStockProducts(int threshold) {
		return productRepository.findByIsActiveTrueAndStockLessThanEqualOrderByStockAsc(threshold);
	}

	@Override
	public long getLowStockCount(int threshold) {
		return productRepository.countByIsActiveTrueAndStockLessThanEqual(threshold);
	}

	private void applyDiscountPricing(Product product) {
		double price = product.getPrice() == null ? 0.0 : product.getPrice();
		int discount = Math.max(0, Math.min(product.getDiscount(), 100));
		product.setDiscount(discount);
		product.setPrice(roundToTwoDecimals(price));
		double discountAmount = price * (discount / 100.0);
		product.setDiscountPrice(roundToTwoDecimals(price - discountAmount));
	}

	private void applyDefaultValues(Product product) {
		if (product.getCostPrice() == null || product.getCostPrice() < 0) {
			product.setCostPrice(product.getPrice() == null ? 0.0 : product.getPrice());
		}
		product.setCostPrice(roundToTwoDecimals(product.getCostPrice()));
		if (product.getAverageRating() == null) {
			product.setAverageRating(0.0);
		}
		if (product.getRatingCount() == null) {
			product.setRatingCount(0);
		}
	}

	private double roundToTwoDecimals(double value) {
		return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

}
