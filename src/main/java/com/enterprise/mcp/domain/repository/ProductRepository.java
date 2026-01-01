package com.enterprise.mcp.domain.repository;

import com.enterprise.mcp.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'accès aux données des produits
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Optional<Product> findByProductCode(String productCode);
    
    List<Product> findByStatus(Product.ProductStatus status);
    
    List<Product> findByCategory(Product.ProductCategory category);
    
    List<Product> findByStatusAndCategory(Product.ProductStatus status, Product.ProductCategory category);
    
    List<Product> findByNameContainingIgnoreCase(String name);
    
    boolean existsByProductCode(String productCode);
}
