package com.ecom.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecom.model.Product;

import jakarta.transaction.Transactional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

	List<Product> findByIsActiveTrue();

	Page<Product> findByIsActiveTrue(Pageable pageable);

	@Query("""
			select p from Product p
			where p.isActive = true
			  and lower(function('replace',
			  	function('replace',
			  		function('replace', trim(p.category), '&', 'and'),
			  		' ',
			  		''
			  	),
			  	'-',
			  	''
			  )) = lower(function('replace',
			  	function('replace',
			  		function('replace', trim(:category), '&', 'and'),
			  		' ',
			  		''
			  	),
			  	'-',
			  	''
			  ))
			order by p.id desc
			""")
	List<Product> findActiveByCategoryNormalized(@Param("category") String category);

	List<Product> findByIsActiveTrueAndCategoryIgnoreCase(String category);

	@Query("""
			select p from Product p
			where p.isActive = true
			  and lower(function('replace',
			  	function('replace',
			  		function('replace', trim(p.category), '&', 'and'),
			  		' ',
			  		''
			  	),
			  	'-',
			  	''
			  )) = lower(function('replace',
			  	function('replace',
			  		function('replace', trim(:category), '&', 'and'),
			  		' ',
			  		''
			  	),
			  	'-',
			  	''
			  ))
			order by p.id desc
			""")
	Page<Product> findActiveByCategoryNormalized(@Param("category") String category, Pageable pageable);

	Page<Product> findByIsActiveTrueAndCategoryIgnoreCase(String category, Pageable pageable);

	@Query("""
			select p from Product p
			where (:keyword = '' or lower(p.title) like lower(concat('%', :keyword, '%'))
				or lower(p.category) like lower(concat('%', :keyword, '%'))
				or lower(p.description) like lower(concat('%', :keyword, '%')))
			order by p.id desc
			""")
	List<Product> searchCatalog(@Param("keyword") String keyword);

	@Query("""
			select p from Product p
			where (:keyword = '' or lower(p.title) like lower(concat('%', :keyword, '%'))
				or lower(p.category) like lower(concat('%', :keyword, '%'))
				or lower(p.description) like lower(concat('%', :keyword, '%')))
			order by p.id desc
			""")
	Page<Product> searchCatalog(@Param("keyword") String keyword, Pageable pageable);

	@Query("""
			select p from Product p
			where p.isActive = true
			  and (:category = '' or lower(function('replace',
			  	function('replace',
			  		function('replace', trim(p.category), '&', 'and'),
			  		' ',
			  		''
			  	),
			  	'-',
			  	''
			  )) = lower(function('replace',
			  	function('replace',
			  		function('replace', trim(:category), '&', 'and'),
			  		' ',
			  		''
			  	),
			  	'-',
			  	''
			  )))
			  and (:keyword = '' or lower(p.title) like lower(concat('%', :keyword, '%'))
				or lower(p.category) like lower(concat('%', :keyword, '%'))
				or lower(p.description) like lower(concat('%', :keyword, '%')))
			order by p.id desc
			""")
	Page<Product> searchActiveCatalog(@Param("category") String category, @Param("keyword") String keyword,
			Pageable pageable);

	List<Product> findByIsActiveTrueAndStockLessThanEqualOrderByStockAsc(Integer stock);

	long countByIsActiveTrueAndStockLessThanEqual(Integer stock);

	@Transactional
	@Modifying
	@Query("""
			update Product p
			set p.category = :newCategory
			where lower(trim(p.category)) = lower(trim(:oldCategory))
			""")
	int updateCategoryName(@Param("oldCategory") String oldCategory, @Param("newCategory") String newCategory);
}
