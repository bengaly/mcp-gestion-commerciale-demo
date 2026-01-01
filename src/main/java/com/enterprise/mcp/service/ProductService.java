package com.enterprise.mcp.service;

import com.enterprise.mcp.domain.entity.Product;
import com.enterprise.mcp.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service métier pour la gestion des produits
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {
    
    private final ProductRepository productRepository;
    
    /**
     * Liste tous les produits
     */
    public List<Product> findAll() {
        return productRepository.findAll();
    }
    
    /**
     * Liste les produits actifs
     */
    public List<Product> findActiveProducts() {
        return productRepository.findByStatus(Product.ProductStatus.ACTIVE);
    }
    
    /**
     * Recherche un produit par son code
     */
    public Optional<Product> findByProductCode(String productCode) {
        log.debug("Recherche produit par code: {}", productCode);
        return productRepository.findByProductCode(productCode);
    }
    
    /**
     * Recherche un produit par son ID
     */
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }
    
    /**
     * Recherche des produits par catégorie
     */
    public List<Product> findByCategory(Product.ProductCategory category) {
        return productRepository.findByCategory(category);
    }
    
    /**
     * Recherche des produits par nom
     */
    public List<Product> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }
    
    /**
     * Crée un nouveau produit
     */
    @Transactional
    public Product createProduct(Product product) {
        log.info("Création du produit: {}", product.getProductCode());
        
        if (productRepository.existsByProductCode(product.getProductCode())) {
            throw new IllegalArgumentException("Un produit avec ce code existe déjà: " + product.getProductCode());
        }
        
        return productRepository.save(product);
    }
    
    /**
     * Met à jour un produit existant
     */
    @Transactional
    public Product updateProduct(Long id, Product productUpdate) {
        log.info("Mise à jour du produit ID: {}", id);
        
        Product existing = productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + id));
        
        existing.setName(productUpdate.getName());
        existing.setDescription(productUpdate.getDescription());
        existing.setCategory(productUpdate.getCategory());
        existing.setUnitPrice(productUpdate.getUnitPrice());
        existing.setStockQuantity(productUpdate.getStockQuantity());
        existing.setStatus(productUpdate.getStatus());
        existing.setUnit(productUpdate.getUnit());
        
        return productRepository.save(existing);
    }
    
    /**
     * Supprime un produit (soft delete - passage en INACTIVE)
     */
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Désactivation du produit ID: {}", id);
        
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + id));
        
        product.setStatus(Product.ProductStatus.INACTIVE);
        productRepository.save(product);
    }
    
    /**
     * Vérifie si un produit existe et est actif
     */
    public boolean isProductAvailable(String productCode) {
        return productRepository.findByProductCode(productCode)
            .map(p -> p.getStatus() == Product.ProductStatus.ACTIVE)
            .orElse(false);
    }
}
