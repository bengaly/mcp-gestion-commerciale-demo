package com.enterprise.mcp.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Entité Ligne de facture - Détail d'un article dans une facture
 */
@Entity
@Table(name = "invoice_lines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLine {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;
    
    @Column(nullable = false)
    private String description;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private BigDecimal unitPrice;
    
    private BigDecimal discountPercent;
    
    private BigDecimal lineTotal;
    
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
