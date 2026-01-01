package com.enterprise.mcp.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité Facture - Représente une facture liée à une commande
 */
@Entity
@Table(name = "invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String invoiceNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;
    
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceLine> lines = new ArrayList<>();
    
    private BigDecimal subtotalAmount;
    
    private BigDecimal taxAmount;
    
    private BigDecimal totalAmount;
    
    private BigDecimal paidAmount;
    
    private BigDecimal remainingAmount;
    
    private LocalDate issueDate;
    
    private LocalDate dueDate;
    
    private LocalDate paidDate;
    
    private String billingAddress;
    
    private String paymentTerms;
    
    private String notes;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (issueDate == null) {
            issueDate = LocalDate.now();
        }
        if (dueDate == null) {
            dueDate = issueDate.plusDays(30);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public void addLine(InvoiceLine line) {
        lines.add(line);
        line.setInvoice(this);
    }
    
    public void calculateTotals() {
        this.subtotalAmount = lines.stream()
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
            this.taxAmount = this.subtotalAmount.multiply(new BigDecimal("0.20"));
        }
        
        this.totalAmount = this.subtotalAmount.add(this.taxAmount);
        
        if (this.paidAmount == null) {
            this.paidAmount = BigDecimal.ZERO;
        }
        
        this.remainingAmount = this.totalAmount.subtract(this.paidAmount);
    }
    
    public boolean isOverdue() {
        return status != InvoiceStatus.PAID 
            && dueDate != null 
            && LocalDate.now().isAfter(dueDate);
    }
    
    public long getDaysOverdue() {
        if (!isOverdue()) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    }
    
    public enum InvoiceStatus {
        DRAFT,
        ISSUED,
        SENT,
        PARTIALLY_PAID,
        PAID,
        OVERDUE,
        CANCELLED,
        DISPUTED
    }
}
