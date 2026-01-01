package com.enterprise.mcp.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour la création d'une commande
 * 
 * Ce DTO est utilisé par les capacités MCP pour recevoir les demandes
 * de création de commande de manière structurée et validable.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    @NotBlank(message = "Le code client est obligatoire")
    private String customerCode;
    
    @NotEmpty(message = "Au moins une ligne de commande est requise")
    @Valid
    private List<OrderLineRequest> lines;
    
    private String shippingAddress;
    
    private String billingAddress;
    
    private String notes;
    
    private LocalDateTime expectedDeliveryDate;
    
    /**
     * DTO pour une ligne de commande
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderLineRequest {
        
        @NotBlank(message = "Le code produit est obligatoire")
        private String productCode;
        
        @NotBlank(message = "Le nom du produit est obligatoire")
        private String productName;
        
        private Integer quantity;
        
        private BigDecimal unitPrice;
        
        private BigDecimal discountPercent;
        
        private String notes;
    }
    
    /**
     * Génère un résumé de la commande pour confirmation
     */
    public String toConfirmationSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Confirmation de commande ===\n\n");
        sb.append("Client: ").append(customerCode).append("\n");
        
        if (shippingAddress != null) {
            sb.append("Adresse de livraison: ").append(shippingAddress).append("\n");
        }
        
        sb.append("\nArticles:\n");
        BigDecimal total = BigDecimal.ZERO;
        
        for (OrderLineRequest line : lines) {
            BigDecimal lineTotal = line.getUnitPrice().multiply(new BigDecimal(line.getQuantity()));
            if (line.getDiscountPercent() != null && line.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal discount = lineTotal.multiply(line.getDiscountPercent()).divide(new BigDecimal("100"));
                lineTotal = lineTotal.subtract(discount);
            }
            total = total.add(lineTotal);
            
            sb.append("- ").append(line.getProductName())
              .append(" (").append(line.getProductCode()).append(")")
              .append(" x ").append(line.getQuantity())
              .append(" @ ").append(String.format("%,.2f €", line.getUnitPrice()))
              .append(" = ").append(String.format("%,.2f €", lineTotal))
              .append("\n");
        }
        
        sb.append("\nTotal HT: ").append(String.format("%,.2f €", total));
        sb.append("\nTVA (20%): ").append(String.format("%,.2f €", total.multiply(new BigDecimal("0.20"))));
        sb.append("\nTotal TTC: ").append(String.format("%,.2f €", total.multiply(new BigDecimal("1.20"))));
        
        if (notes != null && !notes.isBlank()) {
            sb.append("\n\nNotes: ").append(notes);
        }
        
        return sb.toString();
    }
}
