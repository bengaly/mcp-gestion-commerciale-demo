package com.enterprise.mcp.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité Client - Représente un client dans le système commercial
 */
@Entity
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String customerCode;
    
    @Column(nullable = false)
    private String companyName;
    
    private String contactName;
    
    private String email;
    
    private String phone;
    
    private String address;
    
    private String city;
    
    private String country;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerStatus status;
    
    @Enumerated(EnumType.STRING)
    private CustomerSegment segment;
    
    private Double creditLimit;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum CustomerStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        PROSPECT
    }
    
    public enum CustomerSegment {
        STANDARD,
        PREMIUM,
        VIP,
        ENTERPRISE
    }
}
