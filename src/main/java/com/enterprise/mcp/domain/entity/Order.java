package com.enterprise.mcp.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité Commande - Représente une commande client
 */
@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String orderNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderLine> lines = new ArrayList<>();
    
    private BigDecimal totalAmount;
    
    private BigDecimal taxAmount;
    
    private BigDecimal discountAmount;
    
    private String shippingAddress;
    
    private String billingAddress;
    
    private String notes;
    
    private LocalDateTime orderDate;
    
    private LocalDateTime expectedDeliveryDate;
    
    private LocalDateTime actualDeliveryDate;
    
    private String createdBy;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public void addLine(OrderLine line) {
        lines.add(line);
        line.setOrder(this);
    }
    
    public void removeLine(OrderLine line) {
        lines.remove(line);
        line.setOrder(null);
    }
    
    public void calculateTotals() {
        this.totalAmount = lines.stream()
            .map(line -> {
                // Si lineTotal est null, le calculer manuellement
                if (line.getLineTotal() == null) {
                    BigDecimal subtotal = line.getUnitPrice().multiply(new BigDecimal(line.getQuantity()));
                    if (line.getDiscountPercent() != null && line.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal discount = subtotal.multiply(line.getDiscountPercent()).divide(new BigDecimal("100"));
                        subtotal = subtotal.subtract(discount);
                    }
                    return subtotal;
                }
                return line.getLineTotal();
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (this.taxAmount == null) {
            this.taxAmount = this.totalAmount.multiply(new BigDecimal("0.20"));
        }
    }
    
    public enum OrderStatus {
        DRAFT,
        PENDING_VALIDATION,
        VALIDATED,
        IN_PREPARATION,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        RETURNED
    }
}
