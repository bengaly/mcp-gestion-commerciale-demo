package com.enterprise.mcp.service.dto;

import com.enterprise.mcp.domain.entity.Customer;
import com.enterprise.mcp.domain.entity.Invoice;
import com.enterprise.mcp.domain.entity.Order;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO résumant l'activité complète d'un client
 * 
 * Ce DTO est conçu pour être consommé par les capacités MCP.
 * Il agrège les informations pertinentes sans exposer les détails internes.
 */
@Data
@Builder
public class CustomerActivitySummary {
    
    private Customer customer;
    
    // Statistiques commandes
    private int totalOrders;
    private BigDecimal totalRevenue;
    private List<Order> recentOrders;
    
    // Statistiques factures
    private int totalInvoices;
    private BigDecimal totalPaid;
    private BigDecimal totalOutstanding;
    private int unpaidInvoicesCount;
    private List<Invoice> recentInvoices;
    private boolean hasOverdueInvoices;
    
    // Métadonnées
    private LocalDateTime generatedAt;
    
    /**
     * Génère un résumé textuel pour l'IA
     */
    public String toNaturalLanguageSummary() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("=== Résumé Client: ").append(customer.getCompanyName()).append(" ===\n\n");
        
        // Informations générales
        sb.append("**Informations générales:**\n");
        sb.append("- Code client: ").append(customer.getCustomerCode()).append("\n");
        sb.append("- Segment: ").append(customer.getSegment()).append("\n");
        sb.append("- Statut: ").append(customer.getStatus()).append("\n");
        sb.append("- Contact: ").append(customer.getContactName()).append("\n");
        sb.append("- Email: ").append(customer.getEmail()).append("\n\n");
        
        // Activité commerciale
        sb.append("**Activité commerciale:**\n");
        sb.append("- Nombre total de commandes: ").append(totalOrders).append("\n");
        sb.append("- Chiffre d'affaires total: ").append(formatCurrency(totalRevenue)).append("\n\n");
        
        // Situation financière
        sb.append("**Situation financière:**\n");
        sb.append("- Nombre total de factures: ").append(totalInvoices).append("\n");
        sb.append("- Montant total payé: ").append(formatCurrency(totalPaid)).append("\n");
        sb.append("- Montant en attente: ").append(formatCurrency(totalOutstanding)).append("\n");
        sb.append("- Factures impayées: ").append(unpaidInvoicesCount).append("\n");
        
        if (hasOverdueInvoices) {
            sb.append("⚠️ ATTENTION: Ce client a des factures en retard de paiement!\n");
        }
        
        // Commandes récentes
        if (!recentOrders.isEmpty()) {
            sb.append("\n**Dernières commandes:**\n");
            recentOrders.stream().limit(3).forEach(order -> {
                sb.append("- ").append(order.getOrderNumber())
                  .append(" | ").append(order.getStatus())
                  .append(" | ").append(formatCurrency(order.getTotalAmount()))
                  .append(" | ").append(order.getOrderDate().toLocalDate())
                  .append("\n");
            });
        }
        
        // Factures récentes
        if (!recentInvoices.isEmpty()) {
            sb.append("\n**Dernières factures:**\n");
            recentInvoices.stream().limit(3).forEach(invoice -> {
                sb.append("- ").append(invoice.getInvoiceNumber())
                  .append(" | ").append(invoice.getStatus())
                  .append(" | ").append(formatCurrency(invoice.getTotalAmount()))
                  .append(" | Échéance: ").append(invoice.getDueDate())
                  .append("\n");
            });
        }
        
        sb.append("\n---\nRésumé généré le: ").append(generatedAt);
        
        return sb.toString();
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0,00 €";
        return String.format("%,.2f €", amount);
    }
}
