package com.enterprise.mcp.service.dto;

import com.enterprise.mcp.domain.entity.Invoice;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO pour l'analyse détaillée d'une facture
 * 
 * Ce DTO est conçu pour être consommé par les capacités MCP.
 * Il fournit une analyse complète avec indicateurs de risque et recommandations.
 */
@Data
@Builder
public class InvoiceAnalysis {
    
    // Référence à la facture originale
    private Invoice invoice;
    
    // Informations de base
    private String invoiceNumber;
    private String customerName;
    private String customerCode;
    private String status;
    
    // Montants
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private BigDecimal paidPercentage;
    
    // Dates
    private LocalDate issueDate;
    private LocalDate dueDate;
    
    // Indicateurs de retard
    private boolean isOverdue;
    private long daysOverdue;
    
    // Évaluation du risque
    private String riskLevel;
    
    // Recommandations
    private List<String> recommendations;
    
    // Contexte client
    private BigDecimal customerTotalPaid;
    private BigDecimal customerTotalOutstanding;
    private int customerInvoiceCount;
    
    /**
     * Génère un rapport d'analyse en langage naturel pour l'IA
     */
    public String toNaturalLanguageReport() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("=== Analyse de la Facture ").append(invoiceNumber).append(" ===\n\n");
        
        // Informations générales
        sb.append("**Client:** ").append(customerName).append(" (").append(customerCode).append(")\n");
        sb.append("**Statut:** ").append(status).append("\n");
        sb.append("**Date d'émission:** ").append(issueDate).append("\n");
        sb.append("**Date d'échéance:** ").append(dueDate).append("\n\n");
        
        // Montants
        sb.append("**Situation financière:**\n");
        sb.append("- Montant total: ").append(formatCurrency(totalAmount)).append("\n");
        sb.append("- Montant payé: ").append(formatCurrency(paidAmount));
        sb.append(" (").append(String.format("%.1f%%", paidPercentage)).append(")\n");
        sb.append("- Reste à payer: ").append(formatCurrency(remainingAmount)).append("\n\n");
        
        // Indicateurs de risque
        sb.append("**Évaluation du risque:** ").append(getRiskEmoji()).append(" ").append(riskLevel).append("\n");
        
        if (isOverdue) {
            sb.append("⚠️ FACTURE EN RETARD DE ").append(daysOverdue).append(" JOURS\n");
        }
        
        sb.append("\n");
        
        // Recommandations
        if (recommendations != null && !recommendations.isEmpty()) {
            sb.append("**Recommandations:**\n");
            recommendations.forEach(r -> sb.append("→ ").append(r).append("\n"));
            sb.append("\n");
        }
        
        // Contexte client
        sb.append("**Historique client:**\n");
        sb.append("- Total facturé (payé): ").append(formatCurrency(customerTotalPaid)).append("\n");
        sb.append("- Encours actuel: ").append(formatCurrency(customerTotalOutstanding)).append("\n");
        sb.append("- Nombre de factures: ").append(customerInvoiceCount).append("\n");
        
        return sb.toString();
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0,00 €";
        return String.format("%,.2f €", amount);
    }
    
    private String getRiskEmoji() {
        return switch (riskLevel) {
            case "AUCUN" -> "✅";
            case "NORMAL" -> "🟢";
            case "ATTENTION" -> "🟡";
            case "FAIBLE" -> "🟠";
            case "MOYEN" -> "🟠";
            case "ÉLEVÉ" -> "🔴";
            case "CRITIQUE" -> "🚨";
            default -> "❓";
        };
    }
}
