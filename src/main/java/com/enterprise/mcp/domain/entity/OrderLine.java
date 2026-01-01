package com.enterprise.mcp.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Entité Ligne de commande - Détail d'un article dans une commande
 */
@Entity
@Table(name = "order_lines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLine {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @Column(nullable = false)
    private String productCode;
    
    @Column(nullable = false)
    private String productName;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private BigDecimal unitPrice;
    
    private BigDecimal discountPercent;
    
    private BigDecimal lineTotal;
    
    private String notes;
    
    @PrePersist
    @PreUpdate
    protected void calculateLineTotal() {
        BigDecimal subtotal = unitPrice.multiply(new BigDecimal(quantity));
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = subtotal.multiply(discountPercent).divide(new BigDecimal("100"));
            subtotal = subtotal.subtract(discount);
        }
        this.lineTotal = subtotal;
    }
}
